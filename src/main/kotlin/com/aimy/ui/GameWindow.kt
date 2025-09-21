package com.aimy.ui

import com.aimy.game.GameMode
import com.aimy.game.GameStats
import com.aimy.settings.AimYSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent

class GameWindow(
    private val gameMode: GameMode,
    private val settings: AimYSettings,
    private val onGameComplete: (GameStats) -> Unit
) : DialogWrapper(true) {

    private val browser: JBCefBrowser = JBCefBrowser()
    private var jsQuery: JBCefJSQuery? = null

    init {
        title = "AimY - Hit the Targets!"
        isModal = true
        init()
        setupGame()
    }

    private fun setupGame() {
        jsQuery = JBCefJSQuery.create(browser)
        jsQuery?.addHandler { message ->
            val msg = message ?: ""
            if (msg.startsWith("AIMY_COMPLETE:")) {
                val payload = msg.removePrefix("AIMY_COMPLETE:")
                val parts = payload.split("|")
                if (parts.size >= 5) {
                    val stats = GameStats(
                        score = parts[0].toIntOrNull() ?: 0,
                        time = parts[1].toIntOrNull() ?: 0,
                        accuracy = parts[2].toIntOrNull() ?: 100,
                        bestStreak = parts[3].toIntOrNull() ?: 0,
                        gameMode = parts[4]
                    )

                    // Ensure UI operations run on EDT to avoid freezing
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            close(OK_EXIT_CODE)
                        } catch (_: Exception) { /* ignore */ }
                        onGameComplete(stats)
                    }
                }
            }
            null
        }
        val gameHtml = generateGameHTML()

        // Inject bridge directly as `aimyMessageHandler` so JS can call it:
        val injectedBridge = jsQuery?.inject("aimyMessageHandler") ?: ""
        val htmlWithBridge = gameHtml.replace(
            "</body>",
            """
            <script>
              $injectedBridge
            </script>
            </body>
            """.trimIndent()
        )

        browser.loadHTML(htmlWithBridge)
    }

    private fun generateGameHTML(): String {
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

                // Game variables
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

                // Game mode setup
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

                // Game timer
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

                // Three.js setup
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

                        // Remove hit target
                        const targetIndex = currentTargets.findIndex(t => t.userData.id === hitResult.target.userData.id);
                        if (targetIndex !== -1) {
                            scene.remove(hitResult.target);
                            currentTargets.splice(targetIndex, 1);
                        }

                        // Check win conditions
                        if (GAME_MODE === "target_rush" && score >= targetCount) {
                            gameComplete();
                        } else if (GAME_MODE === "hydra_targets" && HYDRA_MODE === "target_count" && score >= gameTargetLimit) {
                            gameComplete();
                        } else {
                            // Spawn new targets
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

                    // Send completion message via the injected bridge
                    if (window.aimyMessageHandler) {
                        window.aimyMessageHandler(`AIMY_COMPLETE:${'$'}{score}|${'$'}{finalTime}|${'$'}{finalAccuracy}|${'$'}{bestStreak}|${'$'}{GAME_MODE}`);
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

    override fun createCenterPanel(): JComponent {
        browser.component.preferredSize = Dimension(1200, 800)
        return browser.component
    }

    override fun dispose() {
        jsQuery?.dispose()
        super.dispose()
    }

    override fun createActions(): Array<Action> = emptyArray()
}