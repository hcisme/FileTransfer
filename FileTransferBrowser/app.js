const path = require('path');
const WebSocket = require('ws');
const express = require('express');
const { PORT } = require('./config');
const deviceService = require('./services/device');
const uploadService = require('./services/upload');
const transferRequestService = require('./services/transferRequest');
const { autoChromeBrowser } = require('./tools');

const app = express();
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

deviceService(app);
uploadService(app);

const server = app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);

  // autoChromeBrowser(`http://localhost:${PORT}`);
});

const wss = new WebSocket.Server({ server, path: '/' });
transferRequestService({ wss, app });
