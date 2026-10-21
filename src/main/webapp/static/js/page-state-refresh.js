const authStateScript = document.querySelector("script[data-auth-state]");
const renderedAuthState = authStateScript?.dataset.authState;
const authStatusUrl = authStateScript?.dataset.authStatusUrl;

window.addEventListener("pageshow", async (event) => {
    if (!event.persisted || !renderedAuthState || !authStatusUrl) {
        return;
    }

    try {
        const response = await fetch(authStatusUrl, {
            cache: "no-store",
            credentials: "same-origin"
        });
        if (!response.ok) {
            window.location.reload();
            return;
        }

        const { authState } = await response.json();
        if (authState !== renderedAuthState) {
            window.location.reload();
        }
    } catch (error) {
        window.location.reload();
    }
});
