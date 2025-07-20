const os = require('os');
const path = require('path');
const { exec } = require('child_process');

const TAG = 'IP Utils';

const getLocalIP = () => {
  const localAddresses = Object.values(os.networkInterfaces())
    .flat()
    .filter((i) => i.family === 'IPv4' && !i.internal)
    .map((i) => i.address);

  if (!localAddresses.length) {
    throw new Error(`${TAG}: No local IP address found`);
  }

  return localAddresses[0];
};

const getWindowsDownloadPath = () => path.join(os.homedir(), 'Downloads');

const autoChromeBrowser = (url) => {
  const chromeCommand =
    process.platform === 'win32'
      ? `start chrome "${url}"`
      : process.platform === 'darwin'
      ? `open -a "Google Chrome" "${url}"`
      : `google-chrome "${url}"`;

  exec(chromeCommand, (error) => {
    if (error) {
      console.error('Failed to open browser:', error);
      console.log('Please manually open:', url);
    } else {
      console.log('Opened Google Chrome at:', url);
    }
  });
};

module.exports = {
  getLocalIP,
  getWindowsDownloadPath,
  autoChromeBrowser
};
