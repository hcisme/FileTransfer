const path = require('path');
const express = require('express');
const { PORT } = require('./config');
const deviceService = require('./services/device');
const uploadService = require('./services/upload');
const { autoChromeBrowser } = require('./tools');

const app = express();
app.use(express.static(path.join(__dirname, 'public')));

deviceService(app);
uploadService(app);

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);

  autoChromeBrowser(`http://localhost:${PORT}`);
});
