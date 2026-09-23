// Nimbus Stream player enhancements: remembers playback position per file,
// and shows a friendly "now playing" filename derived from the stream URL.
(() => {
    const video = document.getElementById('player');
    const source = document.getElementById('player-source');
    const nowPlaying = document.getElementById('now-playing');

    if (!video || !source) return;

    const fileId = decodeURIComponent(source.src.split('/stream/')[1] || '');
    const storageKey = fileId ? `nimbus:progress:${fileId}` : null;

    if (fileId) {
        nowPlaying.textContent = `Now playing: ${fileId.split('/').pop()}`;
    }

    // Resume from last watched position, if we have one saved.
    if (storageKey) {
        const savedTime = parseFloat(localStorage.getItem(storageKey));
        if (!Number.isNaN(savedTime) && savedTime > 0) {
            video.addEventListener('loadedmetadata', () => {
                video.currentTime = savedTime;
            }, { once: true });
        }

        // Persist progress periodically and on pause.
        let saveTimer = null;
        const saveProgress = () => localStorage.setItem(storageKey, String(video.currentTime));

        video.addEventListener('timeupdate', () => {
            clearTimeout(saveTimer);
            saveTimer = setTimeout(saveProgress, 1000);
        });
        video.addEventListener('pause', saveProgress);
        video.addEventListener('ended', () => localStorage.removeItem(storageKey));
    }

    // Space to play/pause, arrows to seek ±5s.
    document.addEventListener('keydown', (e) => {
        if (e.target !== document.body) return;

        if (e.code === 'Space') {
            e.preventDefault();
            video.paused ? video.play() : video.pause();
        } else if (e.code === 'ArrowRight') {
            video.currentTime = Math.min(video.currentTime + 5, video.duration || video.currentTime + 5);
        } else if (e.code === 'ArrowLeft') {
            video.currentTime = Math.max(video.currentTime - 5, 0);
        }
    });
})();