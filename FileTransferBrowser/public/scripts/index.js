// 存储扫描到的设备列表
let devices = [];
// 当前选中的设备
let selectedDevice = null;
// 连接选中的ws连接
let selectedDeviceSocket = null;
// 本地ip
let localIp = null;

// DOM元素引用
const statusEl = document.getElementById('status');
const deviceContainer = document.getElementById('deviceContainer');
const selectedDeviceInfo = document.getElementById('selectedDeviceInfo');
const fileInput = document.getElementById('fileInput');
const uploadBtn = document.getElementById('uploadBtn');

// 扫描可用的设备
async function scanDevices() {
  statusEl.textContent = '正在扫描局域网设备...';
  statusEl.className = 'status loading';

  // 添加旋转加载图标
  const spinner = document.createElement('div');
  spinner.className = 'spinner';
  statusEl.appendChild(spinner);

  try {
    const response = await fetch('/list');
    devices = await response.json();

    renderDevices();
    statusEl.textContent = `扫描完成，找到 ${devices.length} 个设备`;
    statusEl.className = 'status success';
  } catch (err) {
    statusEl.textContent = `扫描失败: ${err.message}`;
    statusEl.className = 'status error';
  }
}

// 渲染设备列表
function renderDevices() {
  deviceContainer.innerHTML = '';

  devices.forEach((device) => {
    const ipAddress = device.referer?.address || '未知IP';
    const port = device.port || '未知端口';
    const isAndroid = device.txt && device.txt['platform-android'];
    const deviceType = isAndroid ? 'Android' : device.name.includes('iPad') ? 'iPad' : '其他设备';
    const isSelected = selectedDevice === device;

    const deviceCard = document.createElement('div');
    deviceCard.className = `device-card ${isSelected ? 'selected' : ''}`;
    deviceCard.innerHTML = `
          <div class="device-icon">
            ${isAndroid ? '🤖' : deviceType === 'iPad' ? '🍎' : '💻'}
          </div>
          <div class="device-info">
            <div class="device-name">${device.name}</div>
            <div class="device-ip">${ipAddress}:${port}</div>
            <div class="device-type">${deviceType}</div>
          </div>
          ${isSelected ? '<div style="font-size: 24px;">✓</div>' : ''}
        `;

    deviceCard.addEventListener('click', () => {
      selectedDevice = device;
      connectSelectedDeviceWs();
      updateSelectedDeviceInfo();
      renderDevices();
    });

    deviceContainer.appendChild(deviceCard);
  });

  updateSelectedDeviceInfo();
}

// 更新已选设备信息区域
function updateSelectedDeviceInfo() {
  if (selectedDevice) {
    const ipAddress = selectedDevice.referer?.address || '未知IP';
    const port = selectedDevice.port || '未知端口';

    selectedDeviceInfo.innerHTML = `
          <div style="font-size: 24px;">🔵</div>
          <div style="font-weight: 600;">已选设备:</div>
          <div>${selectedDevice.name} - ${ipAddress}:${port}</div>
        `;
    selectedDeviceInfo.style.display = 'flex';
  } else {
    selectedDeviceInfo.innerHTML = '<div>未选择设备</div>';
    selectedDeviceInfo.style.display = 'none';
  }

  // 更新上传按钮状态
  uploadBtn.disabled = !selectedDevice;
}

function uploadRequest() {
  if (!selectedDeviceSocket) {
    return;
  }

  selectedDeviceSocket.send(
    JSON.stringify({
      type: 'transfer_request',
      requestId: localIp
    })
  );
}

// 文件上传函数
async function uploadFiles() {
  if (!fileInput.files.length) {
    statusEl.textContent = '请先选择要上传的文件';
    statusEl.className = 'status error';
    return;
  }

  if (!selectedDevice) {
    statusEl.textContent = '请先选择目标设备';
    statusEl.className = 'status error';
    return;
  }

  const ipAddress = selectedDevice.referer?.address;
  const port = selectedDevice.port;

  if (!ipAddress || !port) {
    statusEl.textContent = '选中的设备信息不完整';
    statusEl.className = 'status error';
    return;
  }

  const formData = new FormData();
  for (const file of fileInput.files) {
    formData.append('files', file);
  }

  try {
    statusEl.textContent = `正在上传文件到 ${selectedDevice.name}...`;
    statusEl.className = 'status loading';

    // 添加旋转加载图标
    const spinner = document.createElement('div');
    spinner.className = 'spinner';
    statusEl.appendChild(spinner);

    // eslint-disable-next-line no-undef
    const { data: { code, message } = {} } = await axios.post(
      `http://${ipAddress}:${port}/upload`,
      formData,
      {
        onUploadProgress: (progressEvent) => {
          const percent = Math.round((progressEvent.loaded / progressEvent.total) * 100);
          statusEl.textContent = `上传进度: ${percent}%`;
        }
      }
    );

    if (code !== 200) {
      statusEl.textContent = `上传失败: ${message}`;
      statusEl.className = 'status error';
      return;
    }

    statusEl.textContent = '上传成功';
    statusEl.className = 'status success';

    // 重置文件输入
    fileInput.value = '';
  } catch (err) {
    statusEl.textContent = `上传错误: ${err.message}`;
    statusEl.className = 'status error';
  }
}

async function getLocalIp() {
  const response = await fetch('/getIp');
  const data = await response.json();
  localIp = data.ip;
}

function connectSelectedDeviceWs() {
  selectedDeviceSocket?.close();
  const selectedDeviceIp = selectedDevice?.referer?.address;
  if (!selectedDeviceIp) {
    return;
  }
  selectedDeviceSocket = new WebSocket(`ws://${selectedDeviceIp}:${selectedDevice.port}/`);

  selectedDeviceSocket.addEventListener('message', async (event) => {
    const { type, accepted } = JSON.parse(event.data);

    if (type === 'transfer_response') {
      if (accepted) {
        alert(`设备（${selectedDeviceIp}）已接受文件传输请求`);
        uploadFiles();
      } else {
        alert(`设备（${selectedDeviceIp}）拒绝了文件传输请求`);
      }
    }
  });
}

// 获取局域网内ip
getLocalIp();
// 初始化页面时更新已选设备区域
updateSelectedDeviceInfo();
