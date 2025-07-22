const API_BASE_URL = "http://127.0.0.1:8080";
// 侧边栏切换
document.getElementById('sidebar-toggle').addEventListener('click', function () {
    const sidebar = document.querySelector('aside');
    sidebar.classList.toggle('hidden');
    sidebar.classList.toggle('fixed');
    sidebar.classList.toggle('inset-y-0');
    sidebar.classList.toggle('left-0');
    sidebar.classList.toggle('z-50');
});

// 文件上传
const uploadContainer = document.getElementById('upload-container');
const fileUpload = document.getElementById('file-upload');
const uploadProgressContainer = document.getElementById('upload-progress-container');
const uploadProgressBar = document.getElementById('upload-progress-bar');
const uploadPercentage = document.getElementById('upload-percentage');
const uploadFileName = document.getElementById('upload-file-name');
const uploadSuccess = document.getElementById('upload-success');
const uploadError = document.getElementById('upload-error');
const uploadedFileId = document.getElementById('uploaded-file-id');

// 点击上传区域触发文件选择
uploadContainer.addEventListener('click', () => {
    fileUpload.click();
});

// 拖放功能
uploadContainer.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadContainer.classList.add('border-primary');
    uploadContainer.classList.add('bg-primary/5');
});

uploadContainer.addEventListener('dragleave', () => {
    uploadContainer.classList.remove('border-primary');
    uploadContainer.classList.remove('bg-primary/5');
});

uploadContainer.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadContainer.classList.remove('border-primary');
    uploadContainer.classList.remove('bg-primary/5');

    if (e.dataTransfer.files.length > 0) {
        handleFiles(e.dataTransfer.files);
    }
});

// 文件选择变化时处理
fileUpload.addEventListener('change', () => {
    if (fileUpload.files.length > 0) {
        handleFiles(fileUpload.files);
    }
});

// 处理文件上传
function handleFiles(files) {
    // 只处理第一个文件
    const file = files[0];
    if (!file) return;

    // 显示上传进度
    uploadProgressContainer.classList.remove('hidden');
    uploadSuccess.classList.add('hidden');
    uploadError.classList.add('hidden');

    uploadFileName.textContent = file.name;

    // 模拟上传进度
    let progress = 0;
    const interval = setInterval(() => {
        progress += Math.random() * 500;
        if (progress > 100) {
            progress = 100;
            clearInterval(interval);

            // 模拟上传完成，实际应替换为真实的API调用
            setTimeout(() => {
                const formData = new FormData();
                formData.append('file', file);

                fetch(API_BASE_URL + '/data/upload', {
                    method: 'POST',
                    body: formData
                }).then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            uploadedFileId.textContent = data.data;
                            uploadSuccess.classList.remove('hidden');
                            uploadProgressContainer.classList.add('hidden');
                            fetchDocuments();
                        } else {
                            uploadError.classList.remove('hidden');
                            uploadProgressContainer.classList.add('hidden');
                            console.error('上传失败:', error);
                        }
                    })
                    .catch(error => {
                        // 处理错误
                        uploadError.classList.remove('hidden');
                        uploadProgressContainer.classList.add('hidden');
                        console.error('上传失败:', error);
                    });
                // 刷新文档列表
                fetchDocuments();
            }, 800);
        }

        uploadProgressBar.style.width = `${progress}%`;
        uploadPercentage.textContent = `${Math.round(progress)}%`;
    }, 300);
}

// 文档列表相关
const documentsTableBody = document.getElementById('documents-table-body');
const totalDocs = document.getElementById('total-docs');
const newDocs = document.getElementById('new-docs');
const totalSize = document.getElementById('total-size');
const processingDocs = document.getElementById('processing-docs');
const totalDocsPagination = document.getElementById('total-docs-pagination');

// 获取文档列表
function fetchDocuments() {
    // 实际项目中应使用以下代码进行真实的API调用
    fetch(API_BASE_URL + '/data/docs')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                renderDocuments(data.data);
            }
        })
        .catch(error => {
            console.error('获取文档列表失败:', error);
        });
}

function getFileName(fileName, docId) {
    if (!fileName) return '';
    const webIndex = fileName.indexOf('http');
    if (webIndex !== -1) {
        return fileName.substring(0, 10) + "...";
    }
    return fileName.replace(docId + "_", '');
}

function getFileExtension(fileName) {
    if (!fileName) return '';
    const lastDotIndex = fileName.lastIndexOf('.');
    const webIndex = fileName.indexOf('http');
    if (webIndex !== -1) {
        return 'url';
    }
    return lastDotIndex === -1 ? '' : fileName.substring(lastDotIndex + 1);
}

function countSize(fileSize) {
    if (fileSize === 0) return '0 B';

    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(fileSize) / Math.log(1024));

    return `${(fileSize / Math.pow(1024, i)).toFixed(2)} ${units[i]}`;
}

// 渲染文档列表
function renderDocuments(documents) {
    if (documents.length === 0) {
        documentsTableBody.innerHTML = `
          <tr class="text-center">
            <td colspan="7" class="px-4 py-8 text-gray-500">
              <div class="flex flex-col items-center">
                <i class="fa fa-folder-open-o text-4xl mb-3 text-gray-300"></i>
                <p>暂无文档，请上传文档</p>
              </div>
            </td>
          </tr>
        `;
        return;
    }

    let html = '';
    documents.forEach(doc => {
        const statusClass = 'bg-success/10 text-success';
        const fileTypeIcon = getFileTypeIcon(doc.sourceFileName);

        html += `
          <tr class="hover:bg-gray-50 transition-custom">
            <td class="px-4 py-4 whitespace-nowrap">
              <span class="text-primary cursor-pointer hover:underline view-detail" data-id="${doc.docId}">${doc.docId}</span>
            </td>
            <td class="px-4 py-4 whitespace-nowrap">
              <div class="flex items-center">
                <div class="w-8 h-8 rounded bg-gray-100 flex items-center justify-center mr-3">
                  ${fileTypeIcon}
                </div>
                <div>
                  <div class="text-sm font-medium text-gray-900">${getFileName(doc.sourceFileName, doc.docId)}</div>
                </div>
              </div>
            </td>
            <td class="px-4 py-4 whitespace-nowrap">
              <span class="text-sm text-gray-900">${getFileExtension(doc.sourceFileName)}</span>
            </td>
            <td class="px-4 py-4 whitespace-nowrap">
              <span class="text-sm text-gray-900">${countSize(doc.size)}</span>
            </td>
            <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-500">
              ${doc.creationDate}
            </td>
            <td class="px-4 py-4 whitespace-nowrap">
              <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${statusClass}">
                ${doc.keywords}
              </span>
            </td>
            <td class="px-4 py-4 whitespace-nowrap text-right text-sm font-medium">
              <button class="text-primary hover:text-primary/80 mr-3 view-detail" data-id="${doc.docId}">
                <i class="fa fa-eye"></i> 查看
              </button>
              <button class="text-primary hover:text-primary/80 mr-3 vector-detail" data-id="${doc.docId}">
                <i class="fa fa-eye"></i> 查看向量
              </button>
            </td>
          </tr>
        `;
    });

    documentsTableBody.innerHTML = html;
    // 更新统计数据
    totalDocs.textContent = documents.length;
    newDocs.textContent = Math.floor(documents.length * 0.3);
    totalDocsPagination.textContent = documents.length;

    // 计算总大小
    let totalSizeValue = 0;
    documents.forEach(doc => {
        totalSizeValue += doc.size;
    });
    totalSize.textContent = countSize(totalSizeValue);

    // 计算处理中的文档数
    const processing = documents.filter(doc => doc.status !== '已处理').length;
    processingDocs.textContent = "已处理";

    // 添加查看详情事件监听
    document.querySelectorAll('.view-detail').forEach(btn => {
        btn.addEventListener('click', () => {
            const docId = btn.getAttribute('data-id');
            openDocumentDetail(docId);
        });
    });

    // 添加查看向量事件监听
    document.querySelectorAll('.vector-detail').forEach(btn => {
        btn.addEventListener('click', () => {
            const docId = btn.getAttribute('data-id');
            openVectorDetailModal(docId);
        });
    });
}

// 根据文件类型获取图标
function getFileTypeIcon(type) {
    type = type.toLowerCase();
    if (type.includes('word') || type.includes('docx') || type.includes('doc')) {
        return '<i class="fa fa-file-word-o text-blue-600"></i>';
    } else if (type.includes('pdf')) {
        return '<i class="fa fa-file-pdf-o text-red-600"></i>';
    } else if (type.includes('excel') || type.includes('xlsx') || type.includes('xls')) {
        return '<i class="fa fa-file-excel-o text-green-600"></i>';
    } else if (type.includes('powerpoint') || type.includes('ppt') || type.includes('pptx')) {
        return '<i class="fa fa-file-powerpoint-o text-orange-600"></i>';
    } else if (type.includes('text') || type.includes('txt')) {
        return '<i class="fa fa-file-text-o text-gray-600"></i>';
    } else if (type.includes('image') || type.includes('jpg') || type.includes('png') || type.includes('jpeg')) {
        return '<i class="fa fa-file-image-o text-purple-600"></i>';
    } else {
        return '<i class="fa fa-file-o text-gray-600"></i>';
    }
}

// 新增：通过 URL 获取文本
const urlInput = document.getElementById('url-input');
const fetchUrlBtn = document.getElementById('fetch-url-btn');
const urlSuccess = document.getElementById('url-success');
const urlSuccessId = document.getElementById('url-success-id');
const urlError = document.getElementById('url-error');
const urlTypeRadios = document.querySelectorAll('input[name="url-type"]');

fetchUrlBtn.addEventListener('click', async () => {
    const url = urlInput.value;
    if (!url) return;

    urlSuccess.classList.add('hidden');
    urlError.classList.add('hidden');

    let selectedUrlType = 'confluence';
    urlTypeRadios.forEach(radio => {
        if (radio.checked) {
            selectedUrlType = radio.value;
        }
    });

    fetch(API_BASE_URL + '/data/parse?type=' + selectedUrlType + '&url=' + encodeURIComponent(url)).then(response => response.json())
        .then(data => {
            if (data.success) {
                urlSuccessId.textContent = data.data;
                urlSuccess.classList.remove('hidden');
                fetchDocuments();
            } else {
                urlError.classList.remove('hidden');
                console.error('解析失败:', data.message);
            }
        })
        .catch(error => {
            // 处理错误
            urlError.classList.remove('hidden');
            console.error('解析失败:', error);
        });
    // 刷新文档列表
    fetchDocuments();
});


// 文档详情模态框
const documentDetailModal = document.getElementById('document-detail-modal');
const documentDetailLoading = document.getElementById('document-detail-loading');
const documentDetailContent = document.getElementById('document-detail-content');
const documentDetailError = document.getElementById('document-detail-error');
const closeDetailModal = document.getElementById('close-detail-modal');
const closeDetailModalBtn = document.getElementById('close-detail-modal-btn');
const retryLoadDetail = document.getElementById('retry-load-detail');

// 关闭模态框
function closeModal() {
    documentDetailModal.classList.add('hidden');
}

closeDetailModal.addEventListener('click', closeModal);
closeDetailModalBtn.addEventListener('click', closeModal);

// 点击模态框背景关闭
documentDetailModal.addEventListener('click', (e) => {
    if (e.target === documentDetailModal) {
        closeModal();
    }
});

// 打开文档详情
function openDocumentDetail(docId) {
    documentDetailModal.classList.remove('hidden');
    documentDetailLoading.classList.remove('hidden');
    documentDetailContent.classList.add('hidden');
    documentDetailError.classList.add('hidden');

    fetch(API_BASE_URL + `/data/docs/${docId}`)
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                throw new Error('服务器错误');
            }
        })
        .then(data => {
            renderDocumentDetail(data.data);
            documentDetailLoading.classList.add('hidden');
            documentDetailContent.classList.remove('hidden');
        })
        .catch(error => {
            console.error('获取文档详情失败:', error);
            documentDetailLoading.classList.add('hidden');
            documentDetailError.classList.remove('hidden');
        });
}

// 渲染文档详情
function renderDocumentDetail(detail) {
    document.getElementById('detail-file-id').textContent = detail.docId;
    document.getElementById('detail-workspace').textContent = detail.workspace;
    document.getElementById('detail-author').textContent = detail.author;
    document.getElementById('detail-filename').textContent = detail.sourceFileName;
    document.getElementById('detail-creation-date').textContent = detail.creationDate;
    document.getElementById('detail-page-count').textContent = detail.pageCount;
    document.getElementById('detail-char-count').textContent = detail.charCount;
    document.getElementById('detail-word-count').textContent = detail.wordCount;
    document.getElementById('detail-sentence-count').textContent = detail.sentenceCount;
    document.getElementById('detail-keywords').textContent = detail.keywords;
    document.getElementById('process-status').textContent = detail.persisted ? '已向量化' : '等待向量化...';
    // 渲染文本内容
    document.getElementById('detail-text').textContent = detail.text;
}

// 重试加载文档详情
retryLoadDetail.addEventListener('click', () => {
    const fileId = document.getElementById('detail-file-id').textContent;
    if (fileId && fileId !== '-') {
        openDocumentDetail(fileId);
    }
});

// 向量详情模态框相关
const vectorDetailModal = document.getElementById('vector-detail-modal');
const vectorDetailLoading = document.getElementById('vector-detail-loading');
const vectorDetailContent = document.getElementById('vector-detail-content');
const vectorDetailError = document.getElementById('vector-detail-error');
const vectorList = document.getElementById('vector-list');
const deleteAllVectors = document.getElementById('delete-all-vectors');


// 打开向量详情模态框
function openVectorDetailModal(docId) {
    vectorDetailModal.classList.remove('hidden');
    vectorDetailLoading.classList.remove('hidden');
    vectorDetailContent.classList.add('hidden');
    vectorDetailError.classList.add('hidden');
    document.querySelector('.vector-detail').dataset.docId = docId;

    fetch(API_BASE_URL + `/data/vectors/${docId}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                vectorDetailLoading.classList.add('hidden');
                vectorDetailContent.classList.remove('hidden');
                renderVectorList(data.data);
            } else {
                vectorDetailLoading.classList.add('hidden');
                vectorDetailError.classList.remove('hidden');
                console.error('加载向量详情失败:', error);
            }
        })
        .catch(error => {
            vectorDetailLoading.classList.add('hidden');
            vectorDetailError.classList.remove('hidden');
            console.error('加载向量详情失败:', error);
        });
}

// 渲染向量列表
function renderVectorList(vectors) {
    vectorList.innerHTML = '';
    vectors.forEach(vector => {
        const vectorItem = document.createElement('div');
        vectorItem.classList.add('bg-gray-100', 'p-4', 'rounded-lg', 'mb-4');

        const nodeId = document.createElement('p');
        nodeId.textContent = `节点ID: ${vector.nodeId}`;
        vectorItem.appendChild(nodeId);

        /*const vectorValue = document.createElement('p');
        vectorValue.textContent = `向量值: ${vector.vector}`;
        vectorItem.appendChild(vectorValue);*/

        const persisted = document.createElement('p');
        persisted.textContent = `是否持久化: ${vector.persisted ? '是' : '否'}`;
        vectorItem.appendChild(persisted);

        const txt = document.createElement('p');
        txt.textContent = `原始文本: ${vector.txt}`;
        vectorItem.appendChild(txt);

        const deleteButton = document.createElement('button');
        deleteButton.textContent = '删除';
        deleteButton.classList.add('px-4', 'py-2', 'bg-danger', 'text-white', 'rounded-lg', 'hover:bg-danger/90', 'focus:outline-none', 'focus:ring-2', 'focus:ring-offset-2', 'focus:ring-danger', 'mt-2');
        deleteButton.addEventListener('click', () => deleteVector(vector.nodeId, vector.docId));
        vectorItem.appendChild(deleteButton);

        const replaceButton = document.createElement('button');
        replaceButton.textContent = '替换';
        replaceButton.classList.add('px-4', 'py-2', 'bg-primary', 'text-white', 'rounded-lg', 'hover:bg-primary/90', 'focus:outline-none', 'focus:ring-2', 'focus:ring-offset-2', 'focus:ring-primary', 'mt-2', 'ml-2');
        replaceButton.addEventListener('click', () => replaceVector(vector.nodeId, vector.txt, vector.docId));
        vectorItem.appendChild(replaceButton);

        vectorList.appendChild(vectorItem);
    });
}

// 删除单个向量
function deleteVector(nodeId, docId) {
    if (confirm('确定要删除该向量吗？')) {
        fetch(API_BASE_URL + `/data/vectors/${nodeId}`, {
            method: 'DELETE'
        }).then(response => response.json())
            .then(data => {
                openVectorDetailModal(docId);
            })
            .catch(error => {
                alert('删除失败，请稍后再试');
                console.error('删除向量失败:', error);
            });
    }
}

// 替换单个向量
function replaceVector(nodeId, oldTxt, docId) {
    const newTxt = prompt('请输入新的文本（限长300）', oldTxt);
    if (newTxt !== null) {
        if (newTxt.length > 300) {
            alert('输入的文本长度不能超过300');
            return;
        }

        fetch(API_BASE_URL + `/data/vectors/${docId}/${nodeId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: newTxt
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    openVectorDetailModal(docId);
                } else {
                    alert('替换失败，请稍后再试');
                }
            })
            .catch(error => {
                alert('替换失败，请稍后再试');
                console.error('替换向量失败:', error);
            });
    }
}

// 关闭向量详情模态框
document.getElementById('close-vector-modal').addEventListener('click', () => {
    vectorDetailModal.classList.add('hidden');
});
document.getElementById('close-vector-modal-btn').addEventListener('click', () => {
    vectorDetailModal.classList.add('hidden');
});

// 页面加载时获取文档列表
fetchDocuments();