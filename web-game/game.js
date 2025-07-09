const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const startScreen = document.getElementById('startScreen');
const startButton = document.getElementById('startButton');
const scoreEl = document.getElementById('score');

const laneRight = 250; // x coordinate of right lane
const laneLeft = 150;  // x coordinate of left lane
let gameInterval;
let score = 0;

const player = { x: laneRight, y: 520, width: 30, height: 60, color: 'blue', inLeftLane: false };
const truck = { x: laneRight, y: 300, width: 40, height: 100, color: 'brown', panel: 'green' };
let oncomingCars = [];
let keys = {};
let playing = false;

function drawRoad() {
    ctx.fillStyle = '#555';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 2;
    ctx.setLineDash([20, 20]);
    ctx.beginPath();
    ctx.moveTo(canvas.width / 2, 0);
    ctx.lineTo(canvas.width / 2, canvas.height);
    ctx.stroke();
    ctx.setLineDash([]);
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

function spawnOncoming() {
    const car = { x: laneLeft, y: -60, width: 30, height: 60, color: 'red' };
    oncomingCars.push(car);
}

function update() {
    // Truck panel color based on oncoming cars proximity
    truck.panel = 'green';
    for (const car of oncomingCars) {
        if (car.y < truck.y + truck.height/2 + 80 && car.y > truck.y - 200) {
            truck.panel = 'red';
            break;
        }
    }

    if (keys[' ']) {
        player.x = laneLeft;
        player.inLeftLane = true;
        player.y -= 2; // move forward slightly when overtaking
    } else {
        player.x = laneRight;
        player.inLeftLane = false;
    }

    // Move truck slightly
    truck.y += 1;
    if (truck.y > canvas.height + truck.height) {
        truck.y = -truck.height;
    }

    // Move oncoming cars
    for (const car of oncomingCars) {
        car.y += 4;
    }
    oncomingCars = oncomingCars.filter(c => c.y < canvas.height + c.height);

    // Collision detection with oncoming car
    for (const car of oncomingCars) {
        if (player.inLeftLane &&
            Math.abs(player.y - car.y) < (player.height + car.height) / 2) {
            endGame();
            return;
        }
    }

    // Overtake complete
    if (player.inLeftLane && player.y < truck.y - truck.height/2) {
        score += 1;
        scoreEl.textContent = score;
        resetPositions();
    }
}

function resetPositions() {
    player.y = 520;
    truck.y = 300;
    player.inLeftLane = false;
    oncomingCars = [];
}

function draw() {
    drawRoad();
    drawTruck();
    drawCar(player);
    for (const car of oncomingCars) {
        drawCar(car);
    }
}

function gameLoop() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    update();
    draw();
}

function startGame() {
    playing = true;
    startScreen.style.display = 'none';
    resetPositions();
    score = 0;
    scoreEl.textContent = score;
    gameInterval = setInterval(() => {
        spawnOncoming();
    }, 4000 + Math.random()*2000);
    requestAnimationFrame(loop);
}

function loop() {
    if (!playing) return;
    gameLoop();
    requestAnimationFrame(loop);
}

function endGame() {
    playing = false;
    clearInterval(gameInterval);
    startScreen.style.display = 'flex';
}

startButton.addEventListener('click', startGame);

document.addEventListener('keydown', (e) => {
    keys[e.key] = true;
});

document.addEventListener('keyup', (e) => {
    keys[e.key] = false;
});
