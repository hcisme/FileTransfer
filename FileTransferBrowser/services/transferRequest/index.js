const WebSocket = require('ws');

const transferRequests = new Map();
// 120s
const REQUEST_TIMEOUT = 120000;

module.exports = ({ wss, app }) => {
  // 清理过期请求
  setInterval(() => {
    const now = Date.now();
    for (const [id, req] of transferRequests) {
      if (now - req.timestamp > REQUEST_TIMEOUT) {
        transferRequests.delete(id);
        req.ws.close(1001, 'Request timeout');
      }
    }
  }, 30000); // 每30秒清理一次

  wss.on('connection', (ws) => {
    ws.on('message', (data) => {
      try {
        const { type, requestId } = JSON.parse(data);
        if (type === 'transfer_request') {
          // 存储请求信息
          transferRequests.set(requestId, { ws, timestamp: Date.now() });

          // 广播给浏览器客户端
          wss.clients.forEach((client) => {
            const clientIp = client._socket.address().address;

            if (clientIp.includes('127.0.0.1')) {
              if (client.readyState === WebSocket.OPEN) {
                client.send(
                  JSON.stringify({
                    type: 'transfer_request',
                    requestId: requestId
                  })
                );
              }
            }
          });
        }
      } catch (e) {
        console.error('WebSocket error:', e);
      }
    });

    ws.on('close', () => {
      // 清理关联的请求
      for (const [id, req] of transferRequests) {
        if (req.ws === ws) {
          transferRequests.delete(id);
        }
      }
    });
  });

  // 浏览器的接口 用ws发送到android
  app.post('/api/transfer/response', (req, res) => {
    const { requestId, accepted } = req.body;

    if (!transferRequests.has(requestId)) {
      return res.send({ code: 404, message: '请求不存在或已过期' });
    }

    const request = transferRequests.get(requestId);
    try {
      // 转发响应给Android
      request.ws.send(
        JSON.stringify({
          type: 'transfer_response',
          requestId,
          accepted
        })
      );

      transferRequests.delete(requestId);
      res.send({ code: 200, message: '响应已发送' });
    } catch (e) {
      console.error('转发到android失败:', e);
      res.send({ code: 500 , message: '转发失败' });
    }
  });
};
