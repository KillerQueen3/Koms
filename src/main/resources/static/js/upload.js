let file = null;
let url = null;

document.getElementById("image-upload-div").addEventListener("paste", function (event) {
    const items = (event.clipboardData || window.clipboardData).items;
    if (items && items.length) {
        for (let i = 0; i < items.length; i++) {
            if (items[i].type.indexOf('image') !== -1) {
                file = items[i].getAsFile();
                break;
            }
        }
    } else {
        error("不支持！")
        return;
    }
    if (!file) {
        error("无图片！")
    }

    update_preview()
});

document.addEventListener('dragover',function(ev){
    ev.preventDefault();
},false)

document.addEventListener('drop',function(ev){
    ev.preventDefault();
    const df = ev.dataTransfer

    if (df.items !== undefined){
        const item = df.items[0];
        if (item == null) {
            console.log("null")
            return
        }
        if (item.kind === "file") {
            let f = item.getAsFile()
            if (!f.type.startsWith("image")) {
                error("非图片！")
                return;
            }
            file = f
            update_preview()
        } else if (item.kind === "string") {
            item.getAsString(str => {
                fetch(str)
                    .then(response => response.blob())
                    .then(blob => {
                        if (blob != null) {
                            if (!blob.type.startsWith("image")) {
                                error("非图片！")
                                return;
                            }
                            file = blob
                        }
                        update_preview()
                    })
                    .catch(fetchFailed);
                url = str
            })
        }
    } else {
        error("拖拽只支持Chrome!")
    }
},false);

function fetchFailed(str) {
    error("fetch失败 -> 将尝试使用url上传...")
    document.getElementById("image-preview").src = url
    file = null
}

function upload() {
    const val = curGid
    if (val == null || val.length === 0) {
        return
    }
    const r18 = document.getElementById("is-r18").checked

    if (file == null) {
        if (url != null) {
            let xhr = new XMLHttpRequest()
            xhr.open("post", "/bot/sendImgByUrl?group=" + val + "&r18="+ r18 + "&url=" + url, true)
            xhr.setRequestHeader(header, token)
            xhr.send()
            xhr.onload = function () {
                let json = $.parseJSON(xhr.responseText)
                if (json.code === 200) {
                    $("#upload-image-hash").html(json.msg)
                    log_("成功！")
                } else {
                    error(json.msg)
                }
            }
        } else {
            error("空文件！")
        }
        return
    }

    const xhr = new XMLHttpRequest();

    let form = new FormData()
    form.append("file", file)
    form.append("group", val)
    form.append("r18", r18)

    if (xhr.upload) {
        xhr.upload.addEventListener('progress', function (event) {
            log_('处理中... ' + Math.round(event.loaded / event.total * 100) + "%")
        }, false);
    }
    xhr.onload = function () {
        let json = $.parseJSON(xhr.responseText)
        if (json.code === 200) {
            $("#upload-image-hash").html(json.msg)
            log_("成功！")
        } else {
            error(json.msg)
        }
    };
    xhr.onerror = function () {
        error("失败")
    };
    xhr.open('POST', '/bot/uploadFile', true);
    xhr.setRequestHeader(header, token)
    xhr.send(form);
    return true;
}

function log_(str) {
    $("#log").html(str)
}

function error(str) {
    log_('<span style="color:red;">'+ str +'</span>')
}

function update_preview() {
    let preview = document.getElementById("image-preview")
    if (file == null)
        return;
    const reader = new FileReader();
    reader.onload = function() {
        preview.setAttribute("src", window.URL.createObjectURL(file))
    }
    reader.readAsDataURL(file);
}

function upload_file(input) {
    const fs = input.files[0];
    if (fs == null)
        return
    file = fs
    update_preview();
}