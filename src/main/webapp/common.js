function getCookie(){
    const cookieString = document.cookie;
    const cookies = cookieString.split(';');
    for (let cookie of cookies) {
        const [cookieName, cookieValue] = cookie.split('=');
        if (cookieName.trim() === "session::apdc") {
            return cookie;
        }
    }
    return null;
}

function cookieHasTime(){
    const currentTime = Date.now();
    const valueList = getCookieValues(getCookie());
    const cookieCreatedAt = valueList[3];
    const maxAge = valueList[4] * 1000; //convert to millisecs
    const expirationTime = cookieCreatedAt + maxAge;
    return currentTime < expirationTime;
}

function getCookieValues(cookie) {
    let [cookieName, cookieValue] = cookie.split('=');
    return cookieValue.split(".");
}

function formatTimeStamp(timestamp) {
    if (timestamp === null || timestamp === undefined) {
        return 'Timestamp is null or undefined';
    }
    if (!isNaN(timestamp)) {
        let date = new Date(parseInt(timestamp));
        const day = date.getDate().toString().padStart(2, '0');
        const month = (date.getMonth()+1).toString().padStart(2, '0');
        const year = date.getFullYear();
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        const seconds = date.getSeconds().toString().padStart(2, '0');
        return day+"-"+month+"-"+year+" at "+hours+":"+minutes+":"+seconds;
    } else {
        return 'Invalid timestamp';
    }
}

function secondsToHMS(seconds) {
    let hours = Math.floor(seconds / 3600);
    let minutes = Math.floor((seconds % 3600) / 60);
    let remainingSeconds = seconds % 60;

    return (
        (hours < 10 ? "0" : "") +
        hours +
        ":" +
        (minutes < 10 ? "0" : "") +
        minutes +
        ":" +
        (remainingSeconds < 10 ? "0" : "") +
        remainingSeconds
    );
}

function timeout(){
    if (getCookie() == null || !cookieHasTime()) {
        alert("You are not logged in.")
        window.location.href = "/login.html";
    }
}

function stringBreakDown(s){
    const maxLength = 15;
    let contentWithLineBreaks = '';
    for (let i = 0; i < s.length; i += maxLength) {
        contentWithLineBreaks += s.substring(i, i + maxLength) + '\n';
    }
    return contentWithLineBreaks;
}