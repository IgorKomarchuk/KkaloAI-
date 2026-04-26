import os
import re
from feature_translations import feature_trans

source_files = [
    'index.html', '3d-scanner.html', 'ai-coaching.html', 'glp1-companion.html',
    'health-connect.html', 'hidden-fats.html', 'instant-snap.html',
    'streaks.html', 'viral-share.html', 'privacy.html', 'contact.html'
]

# THE ABSOLUTE MASTER DICTIONARY - 100% COVERAGE
trans_map = {
    "🚀 The World's 1st 3D AR Calorie Scanner": {
        'uk': '🚀 Перший у світі 3D AR Сканер Калорій', 'ru': '🚀 Первый в мире 3D AR Сканер Калорий',
        'es': '🚀 El primer escáner de calorías 3D AR del mundo', 'it': '🚀 Il primo scanner di calorie 3D AR al mondo',
        'de': '🚀 Der weltweit erste 3D-AR-Kalorien-Scanner', 'fr': '🚀 Le 1er scanner de calories 3D AR au monde',
        'pl': '🚀 Pierwszy na świecie skaner kalorii 3D AR', 'hi': '🚀 दुनिया का पहला 3D AR कैलोरी स्कैनर'
    },
    "AI Calorie Tracker.": {
        'uk': 'AI Трекер Калорій.', 'ru': 'ИИ Трекер Калорий.', 'es': 'Rastreador de calorías IA.',
        'it': 'Tracker calorie IA.', 'de': 'KI-Kalorien-Tracker.', 'fr': 'Compteur de calories IA.',
        'pl': 'Licznik kalorii AI.', 'hi': 'AI कैलोरी ट्रैकर।'
    },
    "Beyond simple photos. Measure portion volume in 3D, detect hidden cooking oils, and stay compliant with our GLP-1 companion. The ultimate calorie intelligence for 2026.": {
        'uk': 'Більше ніж фото. Міряйте об’єм у 3D та знаходьте приховані олії. Найкращий ШІ для калорій 2026.',
        'ru': 'Больше чем фото. Измеряйте объем в 3D и находите скрытые масла. Лучший ИИ для калорий 2026.',
        'es': 'Más que fotos. Mide el volumen en 3D y detecta aceites ocultos. La inteligencia definitiva para 2026.',
        'it': 'Oltre le foto. Misura il volume in 3D e rileva oli nascosti. L\'intelligence definitiva per il 2026.',
        'de': 'Mehr als nur Fotos. Messen Sie das Volumen in 3D. Die ultimative Kalorien-Intelligenz für 2026.',
        'fr': 'Plus que des photos. Mesurez le volume en 3D. L\'intelligence ultime des calories pour 2026.',
        'pl': 'Więcej niż zdjęcia. Mierz objętość w 3D. Najlepsza inteligencja kaloryczna 2026.',
        'hi': 'साधारण तस्वीरों से परे। 3D में मात्रा मापें। 2026 के लिए बेहतरीन कैलोरी इंटेलिजेंस।'
    },
    "Start Free Trial": {
        'uk': 'Почати безкоштовно', 'ru': 'Начать бесплатно', 'es': 'Prueba gratis',
        'it': 'Prova gratuita', 'de': 'Gratis testen', 'fr': 'Essai gratuit',
        'pl': 'Zacznij okres próbny', 'hi': 'नि:शुल्क परीक्षण शुरू करें'
    },
    "Explore 3D Scanner →": {
        'uk': 'Дослідити 3D Сканер →', 'ru': 'Изучить 3D Сканер →', 'es': 'Escáner 3D →',
        'it': 'Scanner 3D →', 'de': '3D-Scanner →', 'fr': 'Scanner 3D →',
        'pl': 'Skaner 3D →', 'hi': '3D स्कैनर →'
    },
    "Next-Gen Features for Real Success": {
        'uk': 'Функції майбутнього для реального успіху', 'ru': 'Функции будущего для успеха',
        'es': 'Funciones de próxima generación', 'it': 'Funzioni di prossima generazione',
        'de': 'Funktionen der nächsten Generation', 'fr': 'Fonctions de nouvelle génération',
        'pl': 'Funkcje nowej generacji', 'hi': 'सफलता के लिए अगली पीढ़ी की विशेषताएं'
    },
    "The foundation of precision nutrition, built on Gemini 2.5 Flash technology.": {
        'uk': 'Основа прецизійного харчування на базі технології Gemini 2.5 Flash.',
        'ru': 'Основа точного питания на базе Gemini 2.5 Flash.',
        'es': 'La base de la nutrición de precisión, con tecnología Gemini 2.5 Flash.',
        'it': 'La base della nutrizione di precisione, con tecnologia Gemini 2.5 Flash.',
        'de': 'Die Basis für Präzisionsernährung, basierend auf Gemini 2.5 Flash.',
        'fr': 'La base de la nutrition de précision, basée sur Gemini 2.5 Flash.',
        'pl': 'Podstawa precyzyjnego żywienia na bazie technologii Gemini 2.5 Flash.',
        'hi': 'Gemini 2.5 Flash तकनीक पर आधारित सटीक पोषण की नींव।'
    },
    "Instant AI Snap & Track": {
        'uk': 'Миттєве сканування та ШІ', 'ru': 'Мгновенный ИИ Сканер', 'es': 'Escaneo IA instantáneo',
        'it': 'Scansione IA istantanea', 'de': 'Instant KI-Scan', 'fr': 'Scan IA instantané',
        'pl': 'Natychmiastowy skaner AI', 'hi': 'इंस्टेंट AI स्कैन'
    },
    "The core of KkaloAI. Just snap a photo of any meal, and our AI identifies everything on your plate in milliseconds. No manual entry, ever.": {
        'uk': 'Ядро KkaloAI. Сфотографуйте страву, і ШІ розпізнає все на тарілці за мілісекунди.',
        'ru': 'Основа KkaloAI. Сфотографируйте еду, и ИИ распознает все за миллисекунды.',
        'es': 'El núcleo de KkaloAI. Toma una foto y nuestra IA identifica todo en milisegundos.',
        'it': 'Il cuore di KkaloAI. Scatta una foto e la nostra IA identifica tutto in millisecondi.',
        'de': 'Das Herz von KkaloAI. Foto machen und die KI erkennt alles in Millisekunden.',
        'fr': 'Le cœur de KkaloAI. Prenez une photo et notre IA identifie tout en millisecondes.',
        'pl': 'Serce KkaloAI. Zrób zdjęcie, а AI rozpozna wszystko w milisekundy.',
        'hi': 'KkaloAI का मुख्य हिस्सा। भोजन की फोटो लें, और हमारा AI मिलीसेकंड में सब कुछ पहचान लेता है।'
    },
    "3D Portion Measurement": {
        'uk': '3D Вимірювання Порцій', 'ru': '3D Измерение Порций', 'es': 'Medición de porciones 3D',
        'it': 'Misura porzioni 3D', 'de': '3D-Portionsmessung', 'fr': 'Mesure des portions 3D',
        'pl': 'Pomiar porcji 3D', 'hi': '3D पोर्शन मापन'
    },
    "Not just a photo. Use AR to measure the exact volume of your meal. High-precision calories based on physical dimensions, not just visual guesses.": {
        'uk': 'Не просто фото. Використовуйте AR для вимірювання точного об’єму порції.',
        'ru': 'Не просто фото. Используйте AR для измерения точного объема порции.',
        'es': 'No solo una foto. Usa AR para medir el volumen exacto de tu comida.',
        'it': 'Non solo una foto. Usa l\'AR per misurare il volume esatto del pasto.',
        'de': 'Nicht nur ein Foto. Nutzen Sie AR für das exakte Volumen Ihrer Mahlzeit.',
        'fr': 'Pas seulement une photo. Utilisez l\'AR pour mesurer le volume exact.',
        'pl': 'Nie tylko zdjęcie. Użyj AR, aby zmierzyć objętość posiłku.',
        'hi': 'सिर्फ फोटो नहीं। भोजन की मात्रा मापने के लिए AR का उपयोग करें।'
    },
    "GLP-1 Companion": {
        'uk': 'GLP-1 Супутник', 'ru': 'GLP-1 Спутник', 'es': 'Compañero GLP-1',
        'it': 'Companion GLP-1', 'de': 'GLP-1-Begleiter', 'fr': 'Compagnon GLP-1',
        'pl': 'Asystent GLP-1', 'hi': 'GLP-1 सहायक'
    },
    "Specialized mode for those on GLP-1 medications. Track protein priority to protect muscle mass and log symptoms seamlessly.": {
        'uk': 'Режим для тих, хто на GLP-1. Пріоритет білка для м’язів та запис симптомів.',
        'ru': 'Режим для тех, кто на GLP-1. Следите за белком и записывайте симптомы.',
        'es': 'Modo para usuarios de GLP-1. Rastrea proteínas para proteger tu masa muscular.',
        'it': 'Modalità per utenti GLP-1. Traccia le proteine per proteggere la massa muscolare.',
        'de': 'Modus für GLP-1-Nutzer. Proteinpriorität zum Schutz der Muskelmasse.',
        'fr': 'Mode pour utilisateurs GLP-1. Suivi des protéines pour protéger les muscles.',
        'pl': 'Tryb dla osób na GLP-1. Pilnuj białka, aby chronić mięśnie.',
        'hi': 'GLP-1 उपयोगकर्ताओं के लिए विशेष मोड। मांसपेशियों के लिए प्रोटीन ट्रैक करें।'
    },
    "AI Cooking Detection": {
        'uk': 'AI Визначення готування', 'ru': 'ИИ Определение готовки', 'es': 'Detección de cocina IA',
        'it': 'Rilevamento IA', 'de': 'KI-Kocherkennung', 'fr': 'Détection de cuisson IA',
        'pl': 'Wykrywanie AI', 'hi': 'AI कुकिंग डिटेक्शन'
    },
    "Our Gemini 2.5 Flash model identifies hidden fats. Was it steamed, fried, or sauteed? KkaloAI detects the oil you didn't list.": {
        'uk': 'Gemini 2.5 Flash виявляє приховані жири та олії у страві.',
        'ru': 'Gemini 2.5 Flash выявляет скрытые жиры и масла в блюде.',
        'es': 'Nuestro modelo Gemini 2.5 Flash identifica grasas ocultas.',
        'it': 'Il nostro modello Gemini 2.5 Flash identifica grassi nascosti.',
        'de': 'Gemini 2.5 Flash identifiziert versteckte Fette und Öle.',
        'fr': 'Gemini 2.5 Flash identifie les graisses cachées.',
        'pl': 'Gemini 2.5 Flash identyfikuje ukryte tłuszcze.',
        'hi': 'Gemini 2.5 Flash मॉडल छिपे हुए फैट और तेल की पहचान करता है।'
    },
    "Weekly AI Coaching": {
        'uk': 'Щотижневий AI Коучинг', 'ru': 'Еженедельный ИИ Коучинг', 'es': 'Coaching semanal IA',
        'it': 'Coaching settimanale IA', 'de': 'Wöchentliches KI-Coaching', 'fr': 'Coaching hebdomadaire IA',
        'pl': 'Cotygodniowy coaching AI', 'hi': 'साप्ताहिक AI कोचिंग'
    },
    "Your data doesn't just sit there. Every Sunday, get a deep AI analysis of your habits with personalized recipe generated just for you.": {
        'uk': 'Щонеділі отримуйте глибокий AI-аналіз ваших звичок із персоналізованим рецептом.',
        'ru': 'Каждое воскресенье получайте глубокий ИИ-анализ ваших привычек с рецептом.',
        'es': 'Cada domingo, obtén un análisis profundo de la IA con una receta personalizada.',
        'it': 'Ogni domenica, ricevi un\'analisi profonda dell\'IA con una ricetta personalizzata.',
        'de': 'Jeden Sonntag erhalten Sie eine tiefe KI-Analyse mit persönlichem Rezept.',
        'fr': 'Chaque dimanche, recevez une analyse approfondie de l\'IA avec une recette personnalisée.',
        'pl': 'W każdą niedzielę otrzymuj głęboką analizę AI z personalizowanym przepisem.',
        'hi': 'हर रविवार को, एक व्यक्तिगत नुस्खा के साथ अपनी आदतों का गहरा AI विश्लेषण प्राप्त करें।'
    },
    "Daily Streaks": {
        'uk': 'Щоденні серії', 'ru': 'Ежедневные серии', 'es': 'Rachas diarias',
        'it': 'Strisce giornaliere', 'de': 'Tägliche Streaks', 'fr': 'Séries quotidiennes',
        'pl': 'Codzienne serie', 'hi': 'दैनिक स्ट्रीक्स'
    },
    "Built-in gamification to keep you logging. Maintain your fire and watch your discipline (and health) improve every day.": {
        'uk': 'Гейміфікація допоможе вам не пропускати записи. Підтримуйте вогонь дисциплини.',
        'ru': 'Геймификация поможет вам не пропускать записи. Поддерживайте свой огонь.',
        'es': 'Gamificación para que no dejes de registrar. Mantén tu fuego encendido.',
        'it': 'Gamification per non smettere di registrare. Mantieni acceso il tuo fuoco.',
        'de': 'Integrierte Gamification, damit du am Ball bleibst. Halte dein Feuer am brennen.',
        'fr': 'Ludification pour ne pas arrêter d\'enregistrer. Gardez votre feu allumé.',
        'pl': 'Grywalizacja, która pomoże Ci regularnie zapisywać posiłki.',
        'hi': 'आपको लॉगिंग करते रहने के लिए अंतर्निहित गेमिफिकेशन।'
    },
    "Biofeedback Sync": {
        'uk': 'Синхронізація біофідбеку', 'ru': 'Синхронизация биофидбека', 'es': 'Sincronización de biofeedback',
        'it': 'Sincronizzazione biofeedback', 'de': 'Biofeedback-Synchronisierung', 'fr': 'Synchronisation biofeedback',
        'pl': 'Synchronizacja biofeedbacku', 'hi': 'बायोफीडबैक सिंक'
    },
    "Real-time sync with Health Connect. We see your steps, active burns, and weight trends to adjust your daily goals dynamically.": {
        'uk': 'Синхронізація з Health Connect. Крокі, вага та активність оновлюються автоматично.',
        'ru': 'Синхронизация с Health Connect. Шаги, вес и активность обновляются автоматически.',
        'es': 'Sincronización con Health Connect. Pasos, peso y actividad se actualizan.',
        'it': 'Sincronizzazione con Health Connect. Passi, peso e attività si aggiornano.',
        'de': 'Synchronisierung mit Health Connect. Schritte, Gewicht und Aktivität werden aktualisiert.',
        'fr': 'Synchronisation avec Health Connect. Les pas, le poids et l\'activité se mettent à jour.',
        'pl': 'Synchronizacja z Health Connect. Kroki, waga i aktywność są aktualizowane.',
        'hi': 'हेल्थ कनेक्ट के साथ वास्तविक समय सिंक।'
    },
    "Social Viral Loops": {
        'uk': 'Соціальні віральні цикли', 'ru': 'Виральные циклы', 'es': 'Bucles virales sociales',
        'it': 'Cicli virali social', 'de': 'Social Viral Loops', 'fr': 'Boucles virales sociales',
        'pl': 'Społecznościowe cykle wiralowe', 'hi': 'सोशल वायरल लूप'
    },
    "One-tap sharing to TikTok, Reels, and Shorts. Turn your progress into viral content and join the community of thousands.": {
        'uk': 'Поширення в TikTok та Reels одним дотиком. Перетворіть свій прогрес на віральний контент.',
        'ru': 'Публикация в TikTok и Reels одним касанием. Превратите прогресс в контент.',
        'es': 'Comparte en TikTok y Reels con un toque. Convierte tu progreso en contenido.',
        'it': 'Condividi su TikTok e Reels con un tocco. Trasforma i tuoi progressi in contenuti.',
        'de': 'Teilen auf TikTok und Reels mit einem Fingertipp. Mache Fortschritte zu Content.',
        'fr': 'Partagez sur TikTok et Reels en un clic. Transformez vos progrès en contenu.',
        'pl': 'Udostępnianie jednym kliknięciem na TikTok i Reels.',
        'hi': 'टिकटॉक और रील्स पर वन-टैप शेयरिंग।'
    },
    "KkaloAI: AI Calorie Tracker": {
        'uk': 'KkaloAI: ШІ Трекер Калорій', 'ru': 'KkaloAI: ИИ Трекер Калорий', 'es': 'KkaloAI: Rastreador IA',
        'it': 'KkaloAI: Tracker IA', 'de': 'KkaloAI: KI-Kalorientracker', 'fr': 'KkaloAI: Compteur IA',
        'pl': 'KkaloAI: Licznik AI', 'hi': 'KkaloAI: AI कैलोरी ट्रैकर'
    },
    "Snap a photo of any meal and get instant accurate calories & macros in 2 seconds. The smartest AI food scanner for Android in 2026.": {
        'uk': 'Сфотографуйте страву та за 2 секунди отримайте точні калорії. Найрозумніший сканер 2026.',
        'ru': 'Сфотографируйте еду и за 2 секунды получите точные калории. Умнейший сканер 2026.',
        'es': 'Toma una foto y obtén calorías precisas en 2 segundos. El escáner más inteligente de 2026.',
        'it': 'Scatta una foto e ottieni calorie precise in 2 secondi. Lo scanner più intelligente del 2026.',
        'de': 'Foto machen und in 2 Sekunden präzise Kalorien erhalten. Der intelligenteste Scanner 2026.',
        'fr': 'Prenez une photo et obtenez des calories précises en 2 secondes. Le scanner le plus intelligent de 2026.',
        'pl': 'Zrób zdjęcie i otrzymaj kalorie w 2 sekundy. Najmądrzejszy skaner 2026.',
        'hi': 'भोजन की फोटो लें और 2 सेकंड में सटीक कैलोरी प्राप्त करें। 2026 का सबसे स्मार्ट स्कैनर।'
    },
    "Tired of calorie counters that guess wrong or hide the price? KkaloAI solves the biggest problems of Cal AI and other trackers: real accuracy + transparent pricing + native Health Connect.": {
        'uk': 'Набридли лічильники, що помиляються? KkaloAI — це точність + чесна ціна + Health Connect.',
        'ru': 'Надоели счетчики, которые ошибаются? KkaloAI — это точность + честная цена + Health Connect.',
        'es': '¿Cansado de contadores que fallan? KkaloAI es precisión + precio honesto + Health Connect.',
        'it': 'Stanco di contatori che falliscono? KkaloAI è precisione + prezzo onesto + Health Connect.',
        'de': 'Müde von fehlerhaften Zählern? KkaloAI bietet Präzision + ehrlichen Preis + Health Connect.',
        'fr': 'Fatigué des compteurs qui échouent ? KkaloAI c\'est la précision + prix honnête + Health Connect.',
        'pl': 'Masz dość błędnych liczników? KkaloAI to precyzja + uczciwa cena + Health Connect.',
        'hi': 'गलत अनुमान लगाने वाले काउंटरों से थक गए हैं? KkaloAI है सटीकता + ईमानदार कीमत।'
    },
    "Social Viral Loops": {
        'uk': 'Соціальні вірусні цикли', 'ru': 'Виральные циклы',
        'es': 'Ciclos virales sociales', 'it': 'Cicli virali social',
        'de': 'Soziale Viral-Schleifen', 'fr': 'Boucles virales sociales',
        'pl': 'Społecznościowe cykle wiralowe', 'hi': 'सोशल वायरल लूप'
    },
    "Learn More \u2192": {
        'uk': 'Дізнатися більше \u2192', 'ru': 'Узнать больше \u2192',
        'es': 'Saber más \u2192', 'it': 'Scopri di più \u2192',
        'de': 'Mehr erfahren \u2192', 'fr': 'En savoir plus \u2192',
        'pl': 'Dowiedz się więcej \u2192', 'hi': 'अधिक जानें \u2192'
    },
    "Why thousands switched": {
        'uk': 'Чому тисячі переходять до нас', 'ru': 'Почему тысячи переходят к нам', 'es': 'Por qué miles cambiaron',
        'it': 'Perché in migliaia hanno scelto noi', 'de': 'Warum Tausende gewechselt haben', 'fr': 'Pourquoi des milliers ont changé',
        'pl': 'Dlaczego tysiące wybrały nas', 'hi': 'हजारों ने क्यों स्विच किया'
    },
    "Confirm & Adjust — AI shows result + easy portion slider. You stay in control.": {
        'uk': 'Підтвердіть та налаштуйте — ШІ показує результат + зручний повзунок. Ви головний.',
        'ru': 'Подтвердите и настройте — ИИ показывает результат + удобный ползунок. Вы главный.',
        'es': 'Confirmar y Ajustar — La IA muestra el resultado + control de porción. Tú mandas.',
        'it': 'Conferma e Regola — L\'IA mostra il risultato + controllo della porzione. Comandi tu.',
        'de': 'Bestätigen & Anpassen — KI zeigt Ergebnis + Portionsregler. Du hast die Kontrolle.',
        'fr': 'Confirmer et ajuster — L\'IA affiche le résultat + curseur de portion. Vous commandez.',
        'pl': 'Potwierdź i dostosuj — AI pokazuje wynik + suwak porcji. Ty masz kontrolę.',
        'hi': 'पुष्टि करें और समायोजित करें - AI परिणाम + आसान पोर्शन स्लाइडर दिखाता है।'
    },
    "Price shown BEFORE signup — no hidden subscriptions, no 1-star reviews.": {
        'uk': 'Ціна вказана ДО реєстрації — жодних прихованих підписок.',
        'ru': 'Цена указана ДО регистрации — никаких скрытых подписок.',
        'es': 'Precio mostrado ANTES del registro — sin suscripciones ocultas.',
        'it': 'Prezzo mostrato PRIMA della registrazione — nessuna iscrizione nascosta.',
        'de': 'Preis wird VOR der Anmeldung angezeigt — keine versteckten Abos.',
        'fr': 'Prix affiché AVANT l\'inscription — pas d\'abonnements cachés.',
        'pl': 'Cena widoczna PRZED rejestracją — brak ukrytych subskrypcji.',
        'hi': 'साइनअप से पहले कीमत दिखाई गई - कोई छिपी हुई सदस्यता नहीं।'
    },
    "Native Health Connect — automatic two-way sync with Android Health.": {
        'uk': 'Рідний Health Connect — автоматична синхронізація з Android Health.',
        'ru': 'Родной Health Connect — автоматическая синхронизация с Android Health.',
        'es': 'Health Connect nativo — sincronización automática con Android Health.',
        'it': 'Health Connect nativo — sincronizzazione automatica con Android Health.',
        'de': 'Natives Health Connect — automatischer Sync mit Android Health.',
        'fr': 'Health Connect natif — synchronisation automatique avec Android Health.',
        'pl': 'Natywne Health Connect — automatyczna synchronizacja z Android Health.',
        'hi': 'नेटिव हेल्थ कनेक्ट - एंड्रॉइड हेल्थ के साथ स्वचालित सिंक।'
    },
    "Instant AI Scanner — powered by Gemini 2.5 Flash Vision.": {
        'uk': 'Миттєвий ШІ-сканер — на базі Gemini 2.5 Flash Vision.',
        'ru': 'Мгновенный ИИ-сканер — на базе Gemini 2.5 Flash Vision.',
        'es': 'Escáner IA instantáneo — con tecnología Gemini 2.5 Flash Vision.',
        'it': 'Scanner IA istantaneo — con tecnologia Gemini 2.5 Flash Vision.',
        'de': 'Instant KI-Scanner — basierend auf Gemini 2.5 Flash Vision.',
        'fr': 'Scanner IA instantané — basé sur Gemini 2.5 Flash Vision.',
        'pl': 'Natychmiastowy skaner AI — oparty na Gemini 2.5 Flash Vision.',
        'hi': 'इंस्टेंट AI स्कैनर - Gemini 2.5 Flash विजन द्वारा संचालित।'
    },
    "Barcode + OpenFoodFacts — huge database (3M+ products).": {
        'uk': 'Штрих-код + OpenFoodFacts — величезна база (3 млн+ продуктів).',
        'ru': 'Штрих-код + OpenFoodFacts — огромная база (3 млн+ продуктов).',
        'es': 'Código de barras + OpenFoodFacts — base de datos gigante.',
        'it': 'Codice a barre + OpenFoodFacts — database gigante.',
        'de': 'Barcode + OpenFoodFacts — riesige Datenbank (3M+ Produkte).',
        'fr': 'Code-barres + OpenFoodFacts — base de données géante.',
        'pl': 'Kod kreskowy + OpenFoodFacts — ogromna baza produktów.',
        'hi': 'बारकोड + ओपनफूडफैक्ट्स - विशाल डेटाबेस।'
    },
    "Key Features": {
        'uk': 'Ключові особливості', 'ru': 'Ключевые функции', 'es': 'Características clave',
        'it': 'Caratteristiche principali', 'de': 'Hauptmerkmale', 'fr': 'Caractéristiques clés',
        'pl': 'Kluczowe funkcje', 'hi': 'प्रमुख विशेषताएं'
    },
    "AI Food Photo Scanner — photo → calories & macros in seconds": {
        'uk': 'ШІ сканер їжі — фото → калорії та БЖУ за секунди',
        'ru': 'ИИ сканер еды — фото → калории и БЖУ за секунды',
        'es': 'Escáner de fotos IA — foto → calorías en segundos',
        'it': 'Scanner foto IA — foto → calorie in pochi secondi',
        'de': 'KI-Foto-Scanner — Foto → Kalorien in Sekunden',
        'fr': 'Scanner photo IA — photo → calories en quelques secondes',
        'pl': 'Skaner zdjęć AI — zdjęcie → kalorie w kilka sekund',
        'hi': 'AI फूड फोटो स्कैनर - फोटो → कैलोरी सेकंड में'
    },
    "Confirm & Adjust slider for perfect portion control": {
        'uk': 'Повзунок підтвердження та налаштування порції',
        'ru': 'Ползунок подтверждения и настройки порции',
        'es': 'Control deslizante para un ajuste perfecto de porciones',
        'it': 'Cursore per un controllo perfetto delle porzioni',
        'de': 'Schieberegler für perfekte Portionskontrolle',
        'fr': 'Curseur pour un contrôle parfait des portions',
        'pl': 'Suwak dla idealnej kontroli porcji',
        'hi': 'सही पोर्शन नियंत्रण के लिए पुष्टिकरण और समायोजन स्लाइडर'
    },
    "Daily calorie log + macro breakdown": {
        'uk': 'Щоденний журнал калорій та БЖУ', 'ru': 'Дневной журнал калорий и БЖУ',
        'es': 'Registro diario y desglose de macros', 'it': 'Registro giornaliero e macro',
        'de': 'Tägliches Protokoll & Makro-Aufschlüsselung', 'fr': 'Journal quotidien et macros',
        'pl': 'Codzienny dziennik i rozkład makroskładników', 'hi': 'दैनिक कैलोरी लॉग + मैक्रो ब्रेकडाउन'
    },
    "Goal setting: lose weight, gain muscle or maintain": {
        'uk': 'Встановлення цілей: схуднути, м\'язи або підтримання',
        'ru': 'Установка целей: похудение, мышцы или поддержание',
        'es': 'Metas: bajar de peso, ganar músculo o mantener',
        'it': 'Obiettivi: perdere peso, muscoli o mantenimento',
        'de': 'Ziele: Abnehmen, Muskelaufbau oder Halten',
        'fr': 'Objectifs : perte de poids, muscle ou maintien',
        'pl': 'Cele: odchudzanie, budowa mięśni lub utrzymanie',
        'hi': 'लक्ष्य सेटिंग: वजन कम करना, मांसपेशियां बनाना या बनाए रखना'
    },
    "Health Connect integration — Google Fit data sync": {
        'uk': 'Інтеграція з Health Connect — синхронізація Google Fit',
        'ru': 'Интеграция с Health Connect — синхронизация Google Fit',
        'es': 'Integración con Health Connect y Google Fit',
        'it': 'Integrazione con Health Connect e Google Fit',
        'de': 'Health Connect-Integration — Google Fit Sync',
        'fr': 'Intégration Health Connect — Sync Google Fit',
        'pl': 'Integracja z Health Connect — synchronizacja Google Fit',
        'hi': 'हेल्थ कनेक्ट एकीकरण - Google Fit डेटा सिंक'
    },
    "7-day FREE trial — full access, no payment info required": {
        'uk': '7 днів БЕЗКОШТОВНО — повний доступ без оплати',
        'ru': '7 дней БЕСПЛАТНО — полный доступ без оплаты',
        'es': 'Prueba GRATIS de 7 días — sin datos de pago',
        'it': 'Prova GRATUITA di 7 giorni — senza dati di pagamento',
        'de': '7 Tage GRATIS testen — kein Abo-Zwang',
        'fr': 'Essai GRATUIT de 7 jours — sans CB',
        'pl': '7 dni ZA DARMO — pełny dostęp bez opłat',
        'hi': '7-दिवसीय नि:शुल्क परीक्षण'
    },
    "Clean, modern interface built with Jetpack Compose": {
        'uk': 'Чистий інтерфейс на базі Jetpack Compose',
        'ru': 'Чистый интерфейс на базе Jetpack Compose',
        'es': 'Interfaz limpia construida con Jetpack Compose',
        'it': 'Interfaccia pulita in Jetpack Compose',
        'de': 'Sauberes Interface, gebaut mit Jetpack Compose',
        'fr': 'Interface épurée en Jetpack Compose',
        'pl': 'Czysty interfejs w Jetpack Compose',
        'hi': 'Jetpack Compose के साथ बनाया गया आधुनिक इंटरफ़ेस'
    },
    "Coming very soon (v1.1)": {
        'uk': 'Незабаром (v1.1)', 'ru': 'Скоро (v1.1)', 'es': 'Próximamente (v1.1)',
        'it': 'In arrivo (v1.1)', 'de': 'Demnächst verfügbar (v1.1)', 'fr': 'Prochainement (v1.1)',
        'pl': 'Wkrótce (v1.1)', 'hi': 'जल्द ही आ रहा है (v1.1)'
    },
    '🎙️ Voice logging ("I ate two eggs") &nbsp;&bull;&nbsp; 💧 Water tracker &nbsp;&bull;&nbsp; ❤️ Favorite meals &nbsp;&bull;&nbsp; 📈 Weekly progress charts': {
        'uk': '🎙️ Голосовий ввід &bull; 💧 Трекер води &bull; ❤️ Улюблені страви &bull; 📈 Графіки',
        'ru': '🎙️ Голосовой ввод &bull; 💧 Трекер воды &bull; ❤️ Любимые блюда &bull; 📈 Графики',
        'es': '🎙️ Registro por voz &bull; 💧 Rastreador de agua &bull; ❤️ Comidas favoritas',
        'it': '🎙️ Registrazione vocale &bull; 💧 Tracker acqua &bull; ❤️ Pasti preferiti',
        'de': '🎙️ Spracheingabe &bull; 💧 Wasser-Tracker &bull; ❤️ Lieblingsspeisen',
        'fr': '🎙️ Saisie vocale &bull; 💧 Suivi de l\'eau &bull; ❤️ Repas favoris',
        'pl': '🎙️ Logowanie głosowe &bull; 💧 Tracker wody &bull; ❤️ Ulubione posiłki',
        'hi': '🎙️ वॉयस लॉगिंग &bull; 💧 वाटर ट्रैकर &bull; ❤️ पसंदीदा भोजन'
    },
    "Why KkaloAI is better than competitors": {
        'uk': 'Чому KkaloAI краще за конкурентів', 'ru': 'Почему KkaloAI лучше конкурентов',
        'es': 'Por qué KkaloAI es mejor que la competencia', 'it': 'Perché KkaloAI è meglio della concorrenza',
        'de': 'Warum KkaloAI besser als die Konkurrenz ist', 'fr': 'Pourquoi KkaloAI est meilleur que la concurrence',
        'pl': 'Dlaczego KkaloAI jest lepsze od konkurencji', 'hi': 'KkaloAI प्रतिस्पर्धियों से बेहतर क्यों है'
    },
    "While others undercount calories and hide pricing, KkaloAI gives you honest upfront pricing + real accuracy through Confirm & Adjust. Real users say: “Finally an AI calorie tracker that actually gets it right.”": {
        'uk': 'Поки інші приховують ціни, KkaloAI дає чесну вартість та реальну точність.',
        'ru': 'Пока другие скрывают цены, KkaloAI дает честную стоимость и реальную точность.',
        'es': 'Mientras otros ocultan precios, KkaloAI da precios honestos y precisión.',
        'it': 'Mentre altri nascondono i prezzi, KkaloAI offre prezzi onesti e precisione.',
        'de': 'Während andere Preise verstecken, bietet KkaloAI Ehrlichkeit und Präzision.',
        'fr': 'Alors que d\'autres cachent les prix, KkaloAI propose des prix honnêtes.',
        'pl': 'Podczas gdy inni ukrywają ceny, KkaloAI oferuje uczciwość i precyzję.',
        'hi': 'जबकि अन्य कीमतें छिपाते हैं, KkaloAI ईमानदार कीमतें प्रदान करता है।'
    },
    "Perfect for anyone who wants simple, fast and accurate calorie tracking without the frustration. Whether you’re on a calorie deficit, building muscle or just staying mindful — KkaloAI makes food logging effortless.": {
        'uk': 'Ідеально для швидкого та точного обліку калорій. Худніть або наберіть м’язи легко.',
        'ru': 'Идеально для быстрого и точного учета калорий. Худейте или набирайте массу легко.',
        'es': 'Ideal para un seguimiento rápido y preciso. Adelgaza o gana músculo fácilmente.',
        'it': 'Ideale per un monitoraggio rapido e preciso. Perdi peso o aumenta i muscoli.',
        'de': 'Ideal für schnelles und genaues Tracking. Abnehmen oder Muskelaufbau leicht gemacht.',
        'fr': 'Idéal pour un suivi rapide et précis. Perdez du poids ou gagnez du muscle.',
        'pl': 'Idealne do szybkiego i dokładnego liczenia kalorii.',
        'hi': 'सरल, तेज और सटीक कैलोरी ट्रैकिंग के लिए बिल्कुल सही।'
    },
    "Instant AI calorie tracker • Photo calorie counter • Macro AI tracker • Health Connect calorie tracker • Android food scanner": {
        'uk': 'ШІ трекер калорій • Лічильник за фото • Трекер макросів • Health Connect',
        'ru': 'ИИ трекер калорий • Счетчик по фото • Трекер макросов • Health Connect',
        'es': 'Rastreador de calorías IA • Contador por foto • Macros • Health Connect',
        'it': 'Tracker calorie IA • Contatore per foto • Macro • Health Connect',
        'de': 'KI-Kalorientracker • Foto-Kalorienzähler • Makros • Health Connect',
        'fr': 'Compteur de calories IA • Photo-calorimètre • Macros • Health Connect',
        'pl': 'Licznik kalorii AI • Licznik ze zdjęć • Makra • Health Connect',
        'hi': 'AI कैलोरी ट्रैकर • फोटो कैलोरी काउंटर • हेल्थ कनेक्ट'
    },
    "The Premium Experience": {
        'uk': 'Преміум Досвід', 'ru': 'Премиум Опыт', 'es': 'La experiencia premium',
        'it': 'L\'esperienza premium', 'de': 'Das Premium-Erlebnis', 'fr': 'L\'expérience premium',
        'pl': 'Doświadczenie premium', 'hi': 'प्रीमियम अनुभव'
    },
    "7-Day Free Trial. Unlock the 3D Scanner and AI Coach today.": {
        'uk': '7 днів безкоштовно. Спробуйте 3D-сканер та ШІ-коуча вже сьогодні.',
        'ru': '7 дней бесплатно. Попробуйте 3D-сканер и ИИ-коуча уже сегодня.',
        'es': '7 días gratis. Desbloquea el escáner 3D y el entrenador IA.',
        'it': '7 giorni gratis. Sblocca lo scanner 3D e l\'AI coach.',
        'de': '7 Tage gratis. Schalten Sie den 3D-Scanner und KI-Coach frei.',
        'fr': '7 jours gratuits. Débloquez le scanner 3D et le coach IA.',
        'pl': '7 dni za darmo. Odblokuj skaner 3D i trenera AI.',
        'hi': '7-दिवसीय नि:शुल्क परीक्षण। आज ही 3D स्कैनर अनलॉक करें।'
    },
    "Monthly": {
        'uk': 'Щомісячно', 'ru': 'Ежемесячно', 'es': 'Mensual', 'it': 'Mensile',
        'de': 'Monatlich', 'fr': 'Mensuel', 'pl': 'Miesięcznie', 'hi': 'मासिक'
    },
    "Basic AI Scanning": {'uk': 'Базове сканування', 'ru': 'Базовое сканирование', 'es': 'Escaneo básico'},
    "Daily Macros": {'uk': 'Щоденні макроси', 'ru': 'Дневные макросы', 'es': 'Macros diarios'},
    "Health Sync": {'uk': 'Синхронізація здоров’я', 'ru': 'Синхронизация здоровья', 'es': 'Sincronizar salud'},
    "PRO CHOICE": {'uk': 'ВИБІР ПРО', 'ru': 'ВЫБОР ПРО', 'es': 'OPCIÓN PRO', 'it': 'SCELTA PRO', 'de': 'PRO-WAHL', 'fr': 'CHOIX PRO', 'pl': 'WYBÓR PRO', 'hi': 'प्रो पसंद'},
    "Annual": {
        'uk': 'Щорічно', 'ru': 'Ежегодно', 'es': 'Anual', 'it': 'Annuale',
        'de': 'Jährlich', 'fr': 'Annuel', 'pl': 'Rocznie', 'hi': 'वार्षिक'
    },
    "3D AR Measurement": {'uk': '3D AR Вимірювання', 'ru': '3D AR Измерение', 'es': 'Medición 3D AR'},
    "AI Weekly Coach": {'uk': 'AI Щотижневий коуч', 'ru': 'ИИ Еженедельный коуч', 'es': 'Coach semanal IA'},
    "Unlimited Everything": {'uk': 'Безліміт на все', 'ru': 'Безлимит на все', 'es': 'Todo ilimitado'},
    "Privacy Policy": {'uk': 'Політика конфіденційності', 'ru': 'Политика конфиденциальности', 'es': 'Política de privacidad'},
    "Support": {'uk': 'Підтримка', 'ru': 'Поддержка', 'es': 'Soporte'},
    "Partnerships": {'uk': 'Партнерство', 'ru': 'Asociaciones'},
    "&copy; 2026 KkaloAI (SnapCal). Built for the future of nutrition.": {
        'uk': '&copy; 2026 KkaloAI. Створено для майбутнього харчування.',
        'ru': '&copy; 2026 KkaloAI. Создано для будущего питания.',
        'es': '&copy; 2026 KkaloAI. Creado para el futuro de la nutrición.'
    }
}

# Merge feature page translations into main map
trans_map.update(feature_trans)

# Per-language SEO meta
meta_map = {
    'uk': {
        'title': 'KkaloAI: ШІ Трекер Калорій | 3D AR Сканер Їжі 2026',
        'desc':  'KkaloAI — перший у світі 3D AR сканер калорій. Сфотографуйте їжу та отримайте точні калорії за 2 секунди. Режим GLP-1, ШІ Gemini, синхронізація Health Connect. 7 днів безкоштовно.',
        'locale': 'uk_UA'
    },
    'ru': {
        'title': 'KkaloAI: ИИ Трекер Калорий | 3D AR Сканер Еды 2026',
        'desc':  'KkaloAI — первый в мире 3D AR сканер калорий. Сфотографируйте еду и получите точные калории за 2 секунды. Режим GLP-1, ИИ Gemini, синхронизация Health Connect. 7 дней бесплатно.',
        'locale': 'ru_RU'
    },
    'es': {
        'title': 'KkaloAI: Rastreador de Calorías IA | Escáner 3D AR 2026',
        'desc':  'KkaloAI — el primer escáner de calorías 3D AR del mundo. Toma una foto y obtén calorías precisas en 2 segundos. GLP-1, Gemini AI, Health Connect. Prueba gratis 7 días.',
        'locale': 'es_ES'
    },
    'it': {
        'title': 'KkaloAI: Tracker Calorie IA | Scanner 3D AR 2026',
        'desc':  'KkaloAI — il primo scanner di calorie 3D AR al mondo. Scatta una foto e ottieni calorie precise in 2 secondi. GLP-1, Gemini AI, Health Connect. 7 giorni gratuiti.',
        'locale': 'it_IT'
    },
    'de': {
        'title': 'KkaloAI: KI Kalorientracker | 3D AR Lebensmittelscanner 2026',
        'desc':  'KkaloAI — der erste 3D AR Kalorien-Scanner der Welt. Machen Sie ein Foto und erhalten Sie genaue Kalorien in 2 Sekunden. GLP-1, Gemini KI, Health Connect. 7 Tage gratis.',
        'locale': 'de_DE'
    },
    'fr': {
        'title': 'KkaloAI: Compteur de Calories IA | Scanner 3D AR 2026',
        'desc':  'KkaloAI — le premier scanner de calories 3D AR au monde. Prenez une photo et obtenez des calories précises en 2 secondes. GLP-1, Gemini IA, Health Connect. 7 jours gratuits.',
        'locale': 'fr_FR'
    },
    'pl': {
        'title': 'KkaloAI: Licznik Kalorii AI | Skaner 3D AR 2026',
        'desc':  'KkaloAI — pierwszy na świecie skaner kalorii 3D AR. Zrób zdjęcie i uzyskaj dokładne kalorie w 2 sekundy. GLP-1, Gemini AI, Health Connect. 7 dni za darmo.',
        'locale': 'pl_PL'
    },
    'hi': {
        'title': 'KkaloAI: AI कैलोरी ट्रैकर | 3D AR फूड स्कैनर 2026',
        'desc':  'KkaloAI — दुनिया का पहला 3D AR कैलोरी स्कैनर। फोटो लें और 2 सेकंड में सटीक कैलोरी पाएं। GLP-1, Gemini AI, Health Connect। 7 दिन मुफ्त।',
        'locale': 'hi_IN'
    }
}

HREFLANG_BLOCK = """    <link rel="canonical" href="https://kkaloai.com/{lang}/" />
    <link rel="alternate" hreflang="en"        href="https://kkaloai.com/" />
    <link rel="alternate" hreflang="uk"        href="https://kkaloai.com/uk/" />
    <link rel="alternate" hreflang="ru"        href="https://kkaloai.com/ru/" />
    <link rel="alternate" hreflang="es"        href="https://kkaloai.com/es/" />
    <link rel="alternate" hreflang="it"        href="https://kkaloai.com/it/" />
    <link rel="alternate" hreflang="de"        href="https://kkaloai.com/de/" />
    <link rel="alternate" hreflang="fr"        href="https://kkaloai.com/fr/" />
    <link rel="alternate" hreflang="pl"        href="https://kkaloai.com/pl/" />
    <link rel="alternate" hreflang="hi"        href="https://kkaloai.com/hi/" />
    <link rel="alternate" hreflang="x-default" href="https://kkaloai.com/" />"""

def localize_file(content, lang_code):
    html = content
    html = html.replace('<html lang="en">', f'<html lang="{lang_code}">')
    html = html.replace('href="index.css"', 'href="../index.css"')
    html = html.replace('src="language.js"', 'src="../language.js"')
    html = html.replace('src="hero.png"', 'src="../hero.png"')
    html = html.replace('src="favicon.svg"', 'src="../favicon.svg"')

    # Fix favicon href paths for subdirectories
    for fav in ['favicon.ico?v=2', 'favicon-32.png?v=2', 'favicon-192.png?v=2',
                 'favicon.svg?v=2', 'favicon-512.png?v=2']:
        html = html.replace(f'href="{fav}"', f'href="../{fav}"')

    # Adjust image paths
    imgs = ['3d-scanner.png', 'ai-coaching.png', 'glp1-companion.png', 'health-connect.png',
            'hidden-fats.png', 'instant-snap.png', 'streaks.png', 'viral-share.png']
    for img in imgs:
        html = html.replace(f'src="{img}"', f'src="../{img}" loading="lazy"')

    # Add lazy loading to hero image too
    html = html.replace('src="../hero.png"', 'src="../hero.png" loading="eager"')

    # Replace EN canonical/hreflang block with language-specific one
    html = html.replace(
        '    <link rel="canonical" href="https://kkaloai.com/" />\n    <link rel="alternate" hreflang="en"',
        HREFLANG_BLOCK.format(lang=lang_code) + '\n    <!-- EN_HREFLANG_PLACEHOLDER'
    )
    # Clean up the leftover hreflang tags from EN version
    import re as _re
    html = _re.sub(r'    <!-- EN_HREFLANG_PLACEHOLDER.*?hreflang="x-default" href="https://kkaloai\.com/" />', '', html, flags=_re.DOTALL)

    # Inject localized meta title, description, og:locale
    if lang_code in meta_map:
        m = meta_map[lang_code]
        html = html.replace(
            'content="KkaloAI: #1 AI Calorie Tracker | 3D AR Food Scanner 2026"',
            f'content="{m["title"]}"'
        )
        html = html.replace(
            "<title>KkaloAI: #1 AI Calorie Tracker | 3D AR Food Scanner 2026</title>",
            f'<title>{m["title"]}</title>'
        )
        html = html.replace(
            'content="KkaloAI — the world\'s first 3D AR calorie scanner. Snap a photo and get accurate calories &amp; macros in 2 seconds. GLP-1 companion, Gemini AI, Health Connect sync. Free 7-day trial."',
            f'content="{m["desc"]}"'
        )
        html = html.replace('content="en_US"', f'content="{m["locale"]}"')

    # Dropdown logic
    html = html.replace('value="en" selected>', 'value="en">')
    html = html.replace(f'value="{lang_code}"', f'value="{lang_code}" selected')

    # EXACT STR.REPLACE translations - sorted by length descending
    sorted_keys = sorted(trans_map.keys(), key=len, reverse=True)
    for eng in sorted_keys:
        translations = trans_map[eng]
        if lang_code in translations:
            localized = translations[lang_code]
            html = html.replace(eng, localized)

    return html

# RUN BATCH
for lang in ['uk']:
    os.makedirs(lang, exist_ok=True)
    for filename in source_files:
        if not os.path.exists(filename): continue
        with open(filename, 'r', encoding='utf-8') as f:
            content = f.read()
        output = localize_file(content, lang)
        with open(os.path.join(lang, filename), 'w', encoding='utf-8') as f:
            f.write(output)

print("EN + UK build complete.")

