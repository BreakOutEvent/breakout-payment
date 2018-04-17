const {app, BrowserWindow} = require('electron');
const kill = require('tree-kill');
const childProcess = require('child_process');

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let win;
let serverProcess;

// https://github.com/cuba-labs/java-electron-tutorial

let env = process.env;
env.FIDOR_URL = 'https://aps.fidor.de';
env.FIDOR_APM_URL = 'https://aps.fidor.de';
env.FIDOR_CLIENT_ID = '123';
env.FIDOR_CLIENT_SECRET = 'https://aps.fidor.de';
env.BACKEND_URL = 'http://localhost:8082';
env.BACKEND_AUTH_TOKEN = '123';

let appPath = "/bin/breakout-payment-assembly-1.1.0.jar";

if (process.platform === 'win32') {
    serverProcess = childProcess
        .spawn('cmd.exe', ['/c', 'demo.bat'],
            {
                cwd: 'java',
                args: ['-jar', app.getAppPath() + appPath]
            });
} else {
    serverProcess = childProcess
        .spawn('java', ['-jar', app.getAppPath() + appPath], {env: env});

    serverProcess.stdout.on('data', function (data) {
        console.log(data.toString());
    });

    serverProcess.stderr.on('data', function (data) {
        console.log(data.toString());
    });
}

let appUrl = 'http://localhost:1337';


function createWindow() {
    // Create the browser window.
    win = new BrowserWindow({width: 1440, height: 900, title: "BreakOut Fidor Payment"});

    // disable menubar
    win.setMenu(null);

    // and load the index.html of the app.
    win.loadURL(appUrl);

    // Open the DevTools.
    // win.webContents.openDevTools();

    // Emitted when the window is closed.
    win.on('closed', () => {
        // Dereference the window object, usually you would store windows
        // in an array if your app supports multi windows, this is the time
        // when you should delete the corresponding element.
        win = null;
    })
}


const startUp = function () {
    const requestPromise = require('minimal-request-promise');

    requestPromise.get(appUrl).then(function () {
        console.log('Server started!');
        createWindow();
    }, function () {
        console.log('Waiting for the server start...');

        setTimeout(function () {
            startUp();
        }, 200);
    });
};

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', startUp);

// Quit when all windows are closed.
app.on('window-all-closed', () => {
    kill(serverProcess.pid, function () {
        console.log('Server process killed');
        serverProcess = null;
        app.quit();
    });
});

app.on('activate', () => {
    // On macOS it's common to re-create a window in the app when the
    // dock icon is clicked and there are no other windows open.
    if (win === null) {
        startUp()
    }
});

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and require them here.