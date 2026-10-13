(function () {
    var root = document.getElementById('login');
    if (!root) return;
    var button = document.getElementById('continue');
    var status = document.getElementById('status');
    var id = root.dataset.id;

    function show(kind, text) {
        status.className = 'result ' + kind;
        status.textContent = text;
        status.hidden = false;
    }

    function fromBase64Url(value) {
        var base64 = value.replace(/-/g, '+').replace(/_/g, '/');
        var binary = atob(base64 + '='.repeat((4 - base64.length % 4) % 4));
        var bytes = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        return bytes.buffer;
    }

    function toBase64Url(buffer) {
        var bytes = new Uint8Array(buffer);
        var binary = '';
        for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
        return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    }

    // Passkeys need a browser that has them, and a secure (https) page. Until known, the button stays hidden.
    if (window.PublicKeyCredential && window.isSecureContext && navigator.credentials) {
        button.hidden = false;
    } else {
        document.getElementById('unsupported').hidden = false;
        return;
    }

    button.addEventListener('click', async function () {
        button.disabled = true;
        show('info', root.dataset.working);
        try {
            var optionsResponse = await fetch('/generate-auth-challenge', {method: 'POST'});
            if (!optionsResponse.ok) throw new Error('challenge');
            var options = await optionsResponse.json();

            var credential;
            try {
                credential = await navigator.credentials.get({
                    publicKey: {
                        challenge: fromBase64Url(options.challenge),
                        rpId: options.rpId,
                        timeout: options.timeout || 1800000,
                        userVerification: 'required',
                    },
                });
            } catch (e) {
                button.disabled = false;
                return show('error', root.dataset.cancelled);
            }

            var response = credential.response;
            var assertion = {
                id: credential.id,
                rawId: toBase64Url(credential.rawId),
                type: credential.type,
                response: {
                    authenticatorData: toBase64Url(response.authenticatorData),
                    clientDataJSON: toBase64Url(response.clientDataJSON),
                    signature: toBase64Url(response.signature),
                    userHandle: response.userHandle ? toBase64Url(response.userHandle) : null,
                },
                clientExtensionResults: credential.getClientExtensionResults(),
            };
            var result = await fetch('/auth/browser/' + encodeURIComponent(id) + '/complete', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({authenticationResponseJson: JSON.stringify(assertion)}),
            });
            if (!result.ok) throw new Error('complete');

            button.hidden = true;
            show('success', root.dataset.done);
        } catch (e) {
            button.disabled = false;
            show('error', root.dataset.failed);
        }
    });
})();
