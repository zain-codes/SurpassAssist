const startScreen = document.getElementById('startScreen');
const startButton = document.getElementById('startButton');
const scoreEl = document.getElementById('score');
const gameContainer = document.getElementById('gameContainer');

// Enhanced Game constants
const LANE_WIDTH = 4;
const ROAD_WIDTH = 8;
const LANE_LEFT = -LANE_WIDTH / 2;
const LANE_RIGHT = LANE_WIDTH / 2;
const LANE_SWITCH_SPEED = 0.12; // Faster, more responsive
const PLAYER_SPEED = 0.18;
const TRUCK_SPEED = 0.06; // Slower trucks for better gameplay
const ONCOMING_SPEED = 0.5;
const TRUCK_SPAWN_DISTANCE = 50; // More reasonable distance

let scene, camera, renderer;
let player, road;
let trucks = [];
let oncomingCars = [];
let keys = {};
let playing = false;
let score = 0;
let gameInterval;
let truckSpawnTimer = 0;
let difficultyLevel = 1;
let safetySystem = {
    enabled: true,
    warningDistance: 30,
    dangerDistance: 18
};

function init() {
    scene = new THREE.Scene();
    scene.fog = new THREE.Fog(0x87CEEB, 20, 120);

    camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);
    camera.position.set(0, 4, 10);
    camera.lookAt(0, 0, -10);

    renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.setClearColor(0x87CEEB);
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    gameContainer.appendChild(renderer.domElement);

    // Enhanced lighting
    const ambientLight = new THREE.AmbientLight(0x404040, 0.5);
    scene.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, 1.0);
    directionalLight.position.set(-15, 25, 10);
    directionalLight.castShadow = true;
    directionalLight.shadow.mapSize.width = 4096;
    directionalLight.shadow.mapSize.height = 4096;
    directionalLight.shadow.camera.near = 0.5;
    directionalLight.shadow.camera.far = 100;
    directionalLight.shadow.camera.left = -20;
    directionalLight.shadow.camera.right = 20;
    directionalLight.shadow.camera.top = 20;
    directionalLight.shadow.camera.bottom = -20;
    scene.add(directionalLight);

    createEnhancedRoad();
    createEnhancedPlayer();
    createEnhancedUI();

    window.addEventListener('resize', onWindowResize);
}

function createEnhancedRoad() {
    // Main road surface
    const roadGeometry = new THREE.PlaneGeometry(ROAD_WIDTH, 300);
    const roadMaterial = new THREE.MeshLambertMaterial({ 
        color: 0x2a2a2a,
        transparent: true,
        opacity: 0.9
    });
    road = new THREE.Mesh(roadGeometry, roadMaterial);
    road.rotation.x = -Math.PI / 2;
    road.position.y = 0;
    road.receiveShadow = true;
    scene.add(road);

    // Enhanced center line with dashed pattern
    for (let i = -150; i < 150; i += 12) {
        const dashGeometry = new THREE.BoxGeometry(0.2, 0.02, 6);
        const dashMaterial = new THREE.MeshLambertMaterial({ color: 0xffffff });
        const dash = new THREE.Mesh(dashGeometry, dashMaterial);
        dash.position.set(0, 0.01, i);
        scene.add(dash);
    }

    // Road edges with reflective strips
    const edgeGeometry = new THREE.BoxGeometry(0.6, 0.03, 300);
    const edgeMaterial = new THREE.MeshLambertMaterial({ 
        color: 0xffff00,
        emissive: 0x222200
    });
    
    const leftEdge = new THREE.Mesh(edgeGeometry, edgeMaterial);
    leftEdge.position.set(-ROAD_WIDTH/2, 0.015, 0);
    scene.add(leftEdge);

    const rightEdge = new THREE.Mesh(edgeGeometry, edgeMaterial);
    rightEdge.position.set(ROAD_WIDTH/2, 0.015, 0);
    scene.add(rightEdge);

    // Add road surface texture with small markers
    for (let i = -150; i < 150; i += 20) {
        for (let j = -3; j < 4; j += 6) {
            const markerGeometry = new THREE.BoxGeometry(0.08, 0.005, 1);
            const markerMaterial = new THREE.MeshLambertMaterial({ color: 0x444444 });
            const marker = new THREE.Mesh(markerGeometry, markerMaterial);
            marker.position.set(j, 0.0025, i);
            scene.add(marker);
        }
    }
}

function drawCar(car) {
    ctx.fillStyle = car.color;
    ctx.fillRect(car.x - car.width/2, car.y - car.height/2, car.width, car.height);
}

function drawTruck() {
    drawCar(truck);
    // draw panel on back
    ctx.fillStyle = truck.panel === 'green' ? 'lime' : 'red';
    ctx.fillRect(truck.x - 10, truck.y + truck.height/2 - 10, 20, 10);
}

function createEnhancedOncomingCar() {
    const group = new THREE.Group();
    
    // Varied car types
    const carTypes = [
        { color: 0xff0000, size: [1.8, 1.0, 4.0] }, // Sedan
        { color: 0xff4444, size: [2.0, 1.4, 4.5] }, // SUV
        { color: 0xcc0000, size: [1.6, 0.9, 3.5] }, // Compact
        { color: 0x990000, size: [2.2, 1.6, 5.0] }  // Truck
    ];
    
    const carType = carTypes[Math.floor(Math.random() * carTypes.length)];
    
    const bodyGeometry = new THREE.BoxGeometry(...carType.size);
    const bodyMaterial = new THREE.MeshLambertMaterial({ color: carType.color });
    const body = new THREE.Mesh(bodyGeometry, bodyMaterial);
    body.position.y = carType.size[1] / 2;
    body.castShadow = true;
    group.add(body);

    // Enhanced headlights for oncoming cars
    const lightGeometry = new THREE.SphereGeometry(0.15);
    const lightMaterial = new THREE.MeshLambertMaterial({ 
        color: 0xffffaa,
        emissive: 0x666600
    });
    const leftHeadlight = new THREE.Mesh(lightGeometry, lightMaterial);
    const rightHeadlight = new THREE.Mesh(lightGeometry, lightMaterial);
    leftHeadlight.position.set(-0.6, 0.8, carType.size[2]/2 + 0.1);
    rightHeadlight.position.set(0.6, 0.8, carType.size[2]/2 + 0.1);
    group.add(leftHeadlight);
    group.add(rightHeadlight);

    // Wheels for oncoming cars
    const wheelGeometry = new THREE.CylinderGeometry(0.4, 0.4, 0.3);
    const wheelMaterial = new THREE.MeshLambertMaterial({ color: 0x333333 });
    
    const wheelOffset = carType.size[2] * 0.3;
    const wheelPositions = [
        [-0.8, 0.4, wheelOffset], [0.8, 0.4, wheelOffset],
        [-0.8, 0.4, -wheelOffset], [0.8, 0.4, -wheelOffset]
    ];
    
    wheelPositions.forEach(pos => {
        const wheel = new THREE.Mesh(wheelGeometry, wheelMaterial);
        wheel.position.set(pos[0], pos[1], pos[2]);
        wheel.rotation.z = Math.PI / 2;
        wheel.castShadow = true;
        group.add(wheel);
    });

    const car = group;
    car.position.set(LANE_LEFT, 0, player.position.z - 70 - Math.random() * 30);
    car.speed = ONCOMING_SPEED + Math.random() * 0.3;
    car.bounds = {
        width: carType.size[0],
        height: carType.size[1],
        length: carType.size[2]
    };
    
    oncomingCars.push(car);
    scene.add(car);
}

function createEnhancedUI() {
    // Enhanced safety indicator
    const indicatorGeometry = new THREE.BoxGeometry(0.8, 0.5, 0.15);
    const indicatorMaterial = new THREE.MeshLambertMaterial({ 
        color: 0x00ff00,
        emissive: 0x002200
    });
    const indicator = new THREE.Mesh(indicatorGeometry, indicatorMaterial);
    indicator.position.set(4, 3, -2);
    scene.add(indicator);
    
    scene.safetyIndicator = indicator;

    // Add speed indicator
    const speedGeometry = new THREE.BoxGeometry(0.3, 1.5, 0.1);
    const speedMaterial = new THREE.MeshLambertMaterial({ color: 0x0066ff });
    const speedIndicator = new THREE.Mesh(speedGeometry, speedMaterial);
    speedIndicator.position.set(-4, 2, -2);
    scene.add(speedIndicator);
    
    scene.speedIndicator = speedIndicator;
}

function checkCollision(obj1, obj2) {
    const dx = Math.abs(obj1.position.x - obj2.position.x);
    const dy = Math.abs(obj1.position.y - obj2.position.y);
    const dz = Math.abs(obj1.position.z - obj2.position.z);
    
    const minDx = (obj1.bounds.width + obj2.bounds.width) / 2 - 0.2; // Slight tolerance
    const minDy = (obj1.bounds.height + obj2.bounds.height) / 2;
    const minDz = (obj1.bounds.length + obj2.bounds.length) / 2 - 0.3; // Slight tolerance
    
    return dx < minDx && dy < minDy && dz < minDz;
}

function updateEnhancedSafetySystem() {
    let globalDangerLevel = 0;
    
    trucks.forEach(truck => {
        let dangerLevel = 0;
        let closestDistance = Infinity;
        
        // Enhanced safety detection
        for (const car of oncomingCars) {
            const distance = Math.abs(car.position.z - truck.position.z);
            if (distance < closestDistance) {
                closestDistance = distance;
            }
            
            // Dynamic danger zones based on speeds
            const dynamicDangerDistance = safetySystem.dangerDistance + (car.speed * 10);
            const dynamicWarningDistance = safetySystem.warningDistance + (car.speed * 15);
            
            if (distance < dynamicDangerDistance) {
                dangerLevel = 2;
            } else if (distance < dynamicWarningDistance && dangerLevel < 1) {
                dangerLevel = 1;
            }
        }
        
        if (dangerLevel > globalDangerLevel) {
            globalDangerLevel = dangerLevel;
        }
        
        // Enhanced visual feedback
        let panelColor, lightColor, emissiveColor, opacity;
        
        switch (dangerLevel) {
            case 0: // Safe - Bright Green
                panelColor = 0x00ff00;
                lightColor = 0x00ff00;
                emissiveColor = 0x004400;
                opacity = 0.9;
                break;
            case 1: // Warning - Bright Orange
                panelColor = 0xff6600;
                lightColor = 0xff6600;
                emissiveColor = 0x663300;
                opacity = 1.0;
                break;
            case 2: // Danger - Bright Red with flashing effect
                const flash = Math.sin(Date.now() * 0.01) > 0;
                panelColor = flash ? 0xff0000 : 0xaa0000;
                lightColor = flash ? 0xff0000 : 0xaa0000;
                emissiveColor = flash ? 0x660000 : 0x440000;
                opacity = flash ? 1.0 : 0.7;
                break;
        }
        
        truck.panel.material.color.setHex(panelColor);
        truck.panel.material.emissive.setHex(emissiveColor);
        truck.panel.material.opacity = opacity;
        truck.leftLight.material.color.setHex(lightColor);
        truck.rightLight.material.color.setHex(lightColor);
        truck.leftLight.material.emissive.setHex(emissiveColor);
        truck.rightLight.material.emissive.setHex(emissiveColor);
    });
    
    // Update UI indicators
    if (scene.safetyIndicator) {
        const colors = [0x00ff00, 0xff6600, 0xff0000];
        const emissives = [0x002200, 0x331100, 0x440000];
        scene.safetyIndicator.material.color.setHex(colors[globalDangerLevel]);
        scene.safetyIndicator.material.emissive.setHex(emissives[globalDangerLevel]);
    }
    
    return globalDangerLevel;
}

function updateEnhanced() {
    if (!playing) return;

    const dangerLevel = updateEnhancedSafetySystem();
    
    // Enhanced player controls
    if (keys[' '] || keys['ArrowLeft']) {
        if (!player.isChangingLanes) {
            player.targetX = LANE_LEFT;
            player.isChangingLanes = true;
        }
    } else {
        if (!player.isChangingLanes) {
            player.targetX = LANE_RIGHT;
            player.isChangingLanes = true;
        }
    }

    // Smoother lane switching
    const deltaX = player.targetX - player.position.x;
    if (Math.abs(deltaX) > 0.03) {
        player.position.x += deltaX * LANE_SWITCH_SPEED;
    } else {
        player.position.x = player.targetX;
        player.isChangingLanes = false;
        player.inLeftLane = (player.targetX === LANE_LEFT);
    }

    // Enhanced movement logic
    if (player.inLeftLane && !player.isChangingLanes && dangerLevel < 2) {
        player.position.z -= player.speed;
    }

    // Progressive difficulty
    if (score > 0 && score % 50 === 0 && difficultyLevel < 5) {
        difficultyLevel++;
        ONCOMING_SPEED += 0.1;
        safetySystem.dangerDistance *= 0.9;
    }

    // Enhanced truck management
    for (let i = trucks.length - 1; i >= 0; i--) {
        const truck = trucks[i];
        truck.position.z += truck.speed;
        
        // Overtaking logic
        if (player.inLeftLane && 
            Math.abs(player.position.z - truck.position.z) < 18 && 
            player.position.z < truck.position.z) {
            if (!truck.isBeingOvertaken) {
                truck.isBeingOvertaken = true;
            }
        }
        
        // Enhanced scoring
        if (truck.isBeingOvertaken && player.position.z < truck.position.z - 20) {
            score += 15 + (difficultyLevel * 5);
            scoreEl.textContent = score;
            truck.isBeingOvertaken = false;
        }
        
        // Remove distant trucks
        if (truck.position.z > player.position.z + 60) {
            scene.remove(truck);
            trucks.splice(i, 1);
        }
    }

    // Enhanced truck spawning
    truckSpawnTimer++;
    const spawnRate = Math.max(300, 600 - (difficultyLevel * 60));
    if (truckSpawnTimer > spawnRate) {
        const lastTruck = trucks[trucks.length - 1];
        const spawnZ = lastTruck ? lastTruck.position.z - TRUCK_SPAWN_DISTANCE : player.position.z - 40;
        createEnhancedTruck(spawnZ);
        truckSpawnTimer = 0;
    }

    // Enhanced oncoming traffic
    for (let i = oncomingCars.length - 1; i >= 0; i--) {
        const car = oncomingCars[i];
        car.position.z += car.speed;
        
        if (car.position.z > player.position.z + 60) {
            scene.remove(car);
            oncomingCars.splice(i, 1);
        }
    }

    // Enhanced collision detection
    for (const truck of trucks) {
        if (checkCollision(player, truck)) {
            endEnhancedGame();
            return;
        }
    }

    for (const car of oncomingCars) {
        if (checkCollision(player, car)) {
            endEnhancedGame();
            return;
        }
    }

    // Enhanced camera system
    const cameraTargetZ = player.position.z + 12;
    const cameraTargetX = player.position.x * 0.2;
    const cameraTargetY = 4 + Math.abs(player.position.x) * 0.1;
    
    camera.position.x += (cameraTargetX - camera.position.x) * 0.08;
    camera.position.z += (cameraTargetZ - camera.position.z) * 0.08;
    camera.position.y += (cameraTargetY - camera.position.y) * 0.05;
    camera.lookAt(player.position.x, 0, player.position.z - 8);
    
    // Update speed indicator
    if (scene.speedIndicator) {
        const speedScale = player.inLeftLane ? 1.5 : 1.0;
        scene.speedIndicator.scale.y = speedScale;
        scene.speedIndicator.material.color.setHex(player.inLeftLane ? 0xff6600 : 0x0066ff);
    }
}

function animate() {
    requestAnimationFrame(animate);
    updateEnhanced();
    renderer.render(scene, camera);
}

function startEnhancedGame() {
    playing = true;
    startScreen.style.display = 'none';
    
    // Clear existing objects
    trucks.forEach(truck => scene.remove(truck));
    trucks = [];
    oncomingCars.forEach(car => scene.remove(car));
    oncomingCars = [];
    
    // Reset game state
    player.position.set(LANE_RIGHT, 0, 0);
    player.targetX = LANE_RIGHT;
    player.inLeftLane = false;
    player.isChangingLanes = false;
    score = 0;
    difficultyLevel = 1;
    scoreEl.textContent = score;
    truckSpawnTimer = 0;
    
    // Create initial trucks
    createEnhancedTruck(-30);
    createEnhancedTruck(-80);
    createEnhancedTruck(-130);
    
    // Start enhanced traffic system
    gameInterval = setInterval(() => {
        if (playing && oncomingCars.length < (4 + difficultyLevel)) {
            createEnhancedOncomingCar();
        }
    }, Math.max(1000, 2000 - (difficultyLevel * 200)));
}

function endEnhancedGame() {
    playing = false;
    clearInterval(gameInterval);
    startScreen.style.display = 'flex';
    
    camera.position.set(0, 4, 10);
    camera.lookAt(0, 0, -10);
    
    startButton.textContent = `Game Over! Final Score: ${score} - Play Again`;
    setTimeout(() => {
        startButton.textContent = 'Start Enhanced 3D Game';
    }, 4000);
}

function onWindowResize() {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
}

// Event listeners
startButton.addEventListener('click', startEnhancedGame);

document.addEventListener('keydown', (e) => {
    keys[e.key] = true;
});

document.addEventListener('keyup', (e) => {
    keys[e.key] = false;
});

init();
animate();
