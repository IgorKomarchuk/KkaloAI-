(function() {
    // Current page path info
    const fullPath = window.location.pathname;
    const pathParts = fullPath.split('/').filter(p => p);
    const langCodes = ['uk', 'es', 'ru', 'it', 'de', 'fr', 'pl', 'hi'];
    
    // Determine current language from URL
    let currentLang = 'en';
    if (langCodes.includes(pathParts[0])) {
        currentLang = pathParts[0];
    } else if (langCodes.includes(pathParts[pathParts.length - 2])) {
        // Fallback for some hosting structures
        currentLang = pathParts[pathParts.length - 2];
    }

    // Save choice to localStorage
    function setLanguage(lang) {
        localStorage.setItem('kkalo_lang', lang);
        
        // Construct new URL
        const fileName = fullPath.split('/').pop() || 'index.html';
        let newPath = '';
        
        if (lang === 'en') {
            newPath = '/' + fileName;
        } else {
            newPath = '/' + lang + '/' + fileName;
        }
        
        // Clean up relative paths if needed
        newPath = newPath.replace('//', '/');
        
        if (window.location.protocol === 'file:') {
            // Local testing support
             if (lang === 'en') {
                window.location.href = '../' + fileName;
             } else {
                window.location.href = './' + fileName;
             }
             return;
        }

        window.location.href = newPath;
    }

    // Export to global scope
    window.setKkaloLanguage = setLanguage;

    // Automatic redirection logic (optional - only on home page root)
    if (fullPath === '/' || fullPath === '/index.html') {
        const savedLang = localStorage.getItem('kkalo_lang');
        if (savedLang && savedLang !== 'en' && langCodes.includes(savedLang)) {
            // window.location.href = '/' + savedLang + '/index.html';
        }
    }
})();
