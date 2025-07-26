const socketUrl = `ws://${window.location.host}/`;
const socket = new WebSocket(socketUrl);

socket.addEventListener('open', (event) => {
  console.log('WebSocket 连接成功', event);
});

socket.addEventListener('message', async (event) => {
  const { type, requestId } = JSON.parse(event.data);
  console.log('收到消息:', type, requestId);

  if (type === 'transfer_request') {
    const isAllow = confirm(`是否接受（${requestId}）文件传输请求？`);
    fetch('/api/transfer/response', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ requestId, accepted: isAllow })
    });
  }
});

socket.addEventListener('error', (error) => {
  console.error('WebSocket 错误:', error);
});

socket.addEventListener('close', (event) => {
  console.log('WebSocket 关闭:', event);
});
