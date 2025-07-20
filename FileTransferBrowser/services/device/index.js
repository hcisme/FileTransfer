const bonjour = require('bonjour')();
const { PORT } = require('../../config');
const { getLocalIP } = require('../../tools');

const selfIP = getLocalIP();
let discoveredServices = [];

bonjour.publish({
  name: 'pc-browser-service',
  type: 'http',
  port: PORT,
  host: selfIP,
  subtypes: ['_http._tcp'],
  txt: {
    platform: 'node'
  }
});

const browser = bonjour.find({ type: 'http' });

browser.on('up', (service) => {
  const isSelf = service.addresses.some((addr) => selfIP.includes(addr));

  if (!isSelf) {
    discoveredServices.push(service);
  }
});

browser.on('down', (service) => {
  discoveredServices = discoveredServices.filter((s) => s.fqdn !== service.fqdn);
});

module.exports = (app) => {
  app.get('/list', (req, res) => {
    res.json(discoveredServices);
  });
};
