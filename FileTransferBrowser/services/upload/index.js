const multer = require('multer');
const { getWindowsDownloadPath } = require('../../tools');

const downloadPath = getWindowsDownloadPath();

const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      cb(null, downloadPath);
    },
    filename: (req, file, cb) => {
      const buf = Buffer.from(file.originalname, 'latin1');
      const originalName = buf.toString('utf8');

      const timestamped = `${Date.now()}-${originalName}`;
      cb(null, timestamped);
    }
  })
}).array('files', 10);

module.exports = (app) => {
  app.post('/upload', upload, (req, res) => {
    if (!req.files?.length) {
      return res.send({ code: 400, error: 'No files uploaded' });
    }

    const fileInfos = req.files.map((file) => ({
      savedAs: file.filename,
      savedTo: downloadPath
    }));

    res.send({
      code: 200,
      message: 'Files uploaded successfully',
      files: fileInfos
    });
  });
};
