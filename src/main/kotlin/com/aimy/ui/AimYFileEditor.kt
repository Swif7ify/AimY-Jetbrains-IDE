// kotlin
package com.aimy.ui

import com.aimy.game.GameMode
import com.aimy.game.GameStats
import com.aimy.settings.AimYSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent

class AimYFileEditor(
    private val project: Project,
    private val file: VirtualFile
) : FileEditor, UserDataHolder by UserDataHolderBase() {

    private val settings = AimYSettings.getInstance()
    private val browser = JBCefBrowser()
    private var jsQuery: JBCefJSQuery? = null
    private val pcs = PropertyChangeSupport(this)
    private var launched = false

    override fun getComponent(): JComponent = browser.component

    override fun getPreferredFocusedComponent(): JComponent? = browser.component

    override fun getName(): String = "AimY"

    // Required override - return the VirtualFile this editor represents
    override fun getFile(): VirtualFile = file

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun selectNotify() {
        if (launched) return
        launched = true

        val gameMode: GameMode = file.getUserData(AimYFileKeys.GAME_MODE_KEY) ?: GameMode.TARGET_RUSH
        val externalOnComplete: ((GameStats) -> Unit)? = file.getUserData(AimYFileKeys.ON_COMPLETE_KEY)

        ApplicationManager.getApplication().invokeLater {
            // Use the newer JBCefJSQuery constructor
            jsQuery = JBCefJSQuery.create(browser as JBCefBrowser)
            jsQuery?.addHandler { message ->
                val msg = message ?: ""
                if (msg.startsWith("AIMY_COMPLETE:")) {
                    val payload = msg.removePrefix("AIMY_COMPLETE:")
                    val parts = payload.split("|")
                    if (parts.size >= 5) {
                        val stats = GameStats(
                            score = parts.getOrNull(0)?.toIntOrNull() ?: 0,
                            time = parts.getOrNull(1)?.toIntOrNull() ?: 0,
                            accuracy = parts.getOrNull(2)?.toIntOrNull() ?: 100,
                            bestStreak = parts.getOrNull(3)?.toIntOrNull() ?: 0,
                            gameMode = parts.getOrNull(4) ?: gameMode.name
                        )

                        ApplicationManager.getApplication().invokeLater {
                            try {
                                // Invoke external callback first
                                externalOnComplete?.invoke(stats)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            try {
                                // Close the file editor tab
                                FileEditorManager.getInstance(project).closeFile(file)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            // Clean up resources
                            try {
                                jsQuery?.dispose()
                            } catch (_: Exception) {}
                            try {
                                browser.dispose()
                            } catch (_: Exception) {}
                        }
                    }
                }
                null
            }

            val html = generateGameHTML(gameMode, settings)
            val injectedBridge = jsQuery?.inject("aimyMessageHandler") ?: ""
            val htmlWithBridge = html.replace("</body>", "<script>$injectedBridge</script></body>")
            browser.loadHTML(htmlWithBridge)
        }
    }

    override fun deselectNotify() {}

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        pcs.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        pcs.removePropertyChangeListener(listener)
    }

    override fun getBackgroundHighlighter() = null

    override fun getCurrentLocation(): FileEditorLocation? = null

    private fun generateGameHTML(gameMode: GameMode, settings: AimYSettings): String {
        val gameModeStr = when (gameMode) {
            GameMode.TIME_FRENZY -> "time_frenzy"
            GameMode.HYDRA_TARGETS -> "hydra_targets"
            else -> "target_rush"
        }

        val hydraMode = if (settings.hydraMode.lowercase().contains("timed")) "timed" else "target_count"

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>AimY Game</title>
            <style>
                body {
                    margin: 0;
                    background: #1e1e1e;
                    color: #d4d4d4;
                    font-family: 'JetBrains Mono', monospace;
                    overflow: hidden;
                    cursor: crosshair;
                }
                .game-container {
                    width: 100vw;
                    height: 100vh;
                    position: relative;
                }
                .hud {
                    position: fixed;
                    top: 16px;
                    left: 16px;
                    z-index: 1000;
                    display: flex;
                    align-items: center;
                    gap: 18px;
                    padding: 8px 12px;
                    background: rgba(0,0,0,0.28);
                    color: #d4d4d4;
                    border-radius: 10px;
                    backdrop-filter: blur(6px);
                    box-shadow: 0 4px 18px rgba(0,0,0,0.16);
                    font-size: 13px;
                }
                .hud-title {
                    font-size: 13px;
                    font-weight: 700;
                    color: #007acc;
                    padding-right: 12px;
                    border-right: 1px solid rgba(255,255,255,0.1);
                    margin-right: 6px;
                }
                .hud-item {
                    display: inline-flex;
                    align-items: center;
                    gap: 8px;
                    font-weight: 500;
                    color: #d4d4d4;
                }
                .score { color: #00ff41; font-weight: 700; }
                #gameArea { width: 100%; height: 100%; }
            </style>
        </head>
        <body>
            <div class="game-container">
                <div class="hud">
                    <div class="hud-title">AimY</div>
                    <div class="hud-item">Score: <span class="score" id="score">0</span>/<span id="targetTotal">${settings.targetGoals}</span></div>
                    <div class="hud-item">Time: <span id="timer">0</span>s</div>
                    <div class="hud-item">Acc: <span id="accuracy">100</span>%</div>
                    <div class="hud-item">Streak: <span id="streak">0</span></div>
                </div>
                <div id="gameArea"></div>
            </div>

            <script type="module">
                import * as THREE from 'https://unpkg.com/three@0.152.2/build/three.module.js';

                let score = 0;
                let shots = 0;
                let streak = 0;
                let bestStreak = 0;
                let targetCount = ${settings.targetGoals};
                let gameStartTime = Date.now();
                let gameEndTime = null;
                let currentTargets = [];
                let gameTimeLimit = 0;
                let gameTargetLimit = 0;

                const GAME_MODE = "$gameModeStr";
                const HYDRA_MODE = "$hydraMode";

                if (GAME_MODE === "time_frenzy") {
                    gameTimeLimit = ${settings.timeFrenzyDuration};
                    targetCount = 999;
                    document.getElementById('targetTotal').textContent = '∞';
                } else if (GAME_MODE === "hydra_targets") {
                    if (HYDRA_MODE === "timed") {
                        gameTimeLimit = ${settings.hydraTotalTime};
                        targetCount = 999;
                        document.getElementById('targetTotal').textContent = '∞';
                    } else {
                        gameTimeLimit = 0;
                        gameTargetLimit = ${settings.hydraTargetCount};
                        targetCount = gameTargetLimit;
                        document.getElementById('targetTotal').textContent = gameTargetLimit;
                    }
                } else {
                    document.getElementById('targetTotal').textContent = targetCount;
                }

                const scoreElement = document.getElementById('score');
                const timerElement = document.getElementById('timer');
                const accuracyElement = document.getElementById('accuracy');
                const streakElement = document.getElementById('streak');

                let gameTimer = null;
                if (gameTimeLimit > 0) {
                    gameEndTime = gameStartTime + gameTimeLimit;
                    gameTimer = setInterval(() => {
                        const remaining = Math.max(0, Math.ceil((gameEndTime - Date.now()) / 1000));
                        timerElement.textContent = remaining + 's left';
                        if (remaining <= 0) {
                            gameComplete();
                        }
                    }, 100);
                } else {
                    setInterval(() => {
                        const elapsed = Math.floor((Date.now() - gameStartTime) / 1000);
                        timerElement.textContent = elapsed;
                    }, 100);
                }

                const w = window.innerWidth;
                const h = window.innerHeight;
                const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
                renderer.setSize(w, h);
                renderer.setPixelRatio(window.devicePixelRatio || 1);
                document.getElementById('gameArea').appendChild(renderer.domElement);

                const scene = new THREE.Scene();
                const camera = new THREE.OrthographicCamera(w / -2, w / 2, h / 2, h / -2, -1000, 1000);
                camera.position.z = 10;
                scene.add(camera);

                function makeTarget(size = 100) {
                    const group = new THREE.Group();
                    const colors = [0xffffff, 0xff4444, 0xffffff, 0xff4444, 0xffffff, 0xff0000];
                    const normalized = [1.0, 0.8, 0.6, 0.4, 0.2, 0.1];
                    const radii = normalized.map(r => (size / 2) * r);

                    for (let i = 0; i < radii.length; i++) {
                        const outer = radii[i];
                        const inner = (i + 1 < radii.length) ? radii[i + 1] : 0;
                        const geo = new THREE.RingGeometry(inner, outer, 64);
                        const mat = new THREE.MeshBasicMaterial({ color: colors[i], side: THREE.DoubleSide });
                        const mesh = new THREE.Mesh(geo, mat);
                        mesh.position.z = -i * 0.01;
                        group.add(mesh);
                    }

                    group.userData = {
                        isTarget: true,
                        radii: radii,
                        id: Date.now() + Math.random()
                    };

                    return group;
                }

                function createTarget() {
                    const size = ${settings.targetSize};
                    const target = makeTarget(size);
                    const margin = size;
                    const cx = Math.random() * (window.innerWidth - margin * 2) + margin;
                    const cy = Math.random() * (window.innerHeight - margin * 2) + margin;

                    const x = cx - window.innerWidth / 2;
                    const y = window.innerHeight / 2 - cy;
                    target.position.set(x, y, 0);

                    scene.add(target);

                    if (GAME_MODE === "hydra_targets") {
                        currentTargets.push(target);
                    } else {
                        if (currentTargets[0]) scene.remove(currentTargets[0]);
                        currentTargets = [target];
                    }
                }

                function initializeGame() {
                    if (GAME_MODE === "hydra_targets") {
                        for (let i = 0; i < 3; i++) {
                            setTimeout(() => createTarget(), i * 200);
                        }
                    } else {
                        createTarget();
                    }
                }

                function computePointsFromClick(clientX, clientY) {
                    const rect = renderer.domElement.getBoundingClientRect();
                    const canvasX = clientX - rect.left;
                    const canvasY = clientY - rect.top;
                    const worldX = canvasX - rect.width / 2;
                    const worldY = rect.height / 2 - canvasY;

                    for (const target of currentTargets) {
                        const localX = worldX - target.position.x;
                        const localY = worldY - target.position.y;
                        const dist = Math.hypot(localX, localY);
                        const radii = target.userData.radii || [];

                        for (let j = radii.length - 1; j >= 0; j--) {
                            const outer = radii[j];
                            const inner = (j + 1 < radii.length) ? radii[j + 1] : 0;
                            if (dist <= outer && dist >= inner) {
                                return { hit: true, points: 1, target };
                            }
                        }
                    }
                    return { hit: false, points: 0, target: null };
                }

                function onPointerDown(e) {
                    shots++;
                    const hitResult = computePointsFromClick(e.clientX, e.clientY);

                    if (hitResult.hit && hitResult.target) {
                        score += hitResult.points;
                        streak++;
                        if (streak > bestStreak) bestStreak = streak;

                        const targetIndex = currentTargets.findIndex(t => t.userData.id === hitResult.target.userData.id);
                        if (targetIndex !== -1) {
                            scene.remove(hitResult.target);
                            currentTargets.splice(targetIndex, 1);
                        }

                        if (GAME_MODE === "target_rush" && score >= targetCount) {
                            gameComplete();
                        } else if (GAME_MODE === "hydra_targets" && HYDRA_MODE === "target_count" && score >= gameTargetLimit) {
                            gameComplete();
                        } else {
                            if (GAME_MODE === "hydra_targets") {
                                while (currentTargets.length < 3) {
                                    createTarget();
                                }
                            } else {
                                createTarget();
                            }
                        }
                    } else {
                        streak = 0;
                    }

                    updateStats();
                }

                function updateStats() {
                    const accuracy = shots > 0 ? Math.round((score / shots) * 100) : 100;
                    scoreElement.textContent = score;
                    accuracyElement.textContent = accuracy;
                    streakElement.textContent = streak;
                }

                function gameComplete() {
                    const finalTime = Math.floor((Date.now() - gameStartTime) / 1000);
                    const finalAccuracy = shots > 0 ? Math.round((score / shots) * 100) : 100;

                    if (gameTimer) clearInterval(gameTimer);
                    currentTargets.forEach(target => scene.remove(target));
                    currentTargets = [];

                    if (window.aimyMessageHandler) {
                        window.aimyMessageHandler("AIMY_COMPLETE:" + score + "|" + finalTime + "|" + finalAccuracy + "|" + bestStreak + "|" + GAME_MODE);
                    }

                    window.removeEventListener('pointerdown', onPointerDown);
                }

                function animate() {
                    requestAnimationFrame(animate);
                    renderer.render(scene, camera);
                }

                function onResize() {
                    const W = window.innerWidth;
                    const H = window.innerHeight;
                    renderer.setSize(W, H);
                    camera.left = W / -2;
                    camera.right = W / 2;
                    camera.top = H / 2;
                    camera.bottom = H / -2;
                    camera.updateProjectionMatrix();
                }

                window.addEventListener('resize', onResize, false);
                window.addEventListener('pointerdown', onPointerDown, false);

                animate();
                initializeGame();
                updateStats();
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    override fun dispose() {
        try { jsQuery?.dispose() } catch (_: Exception) {}
        try { browser.dispose() } catch (_: Exception) {}
    }

    fun createActions(): Array<javax.swing.Action> = emptyArray()
}