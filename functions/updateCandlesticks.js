const functions = require('firebase-functions');
const admin = require('firebase-admin');
const axios = require('axios');

admin.initializeApp();

exports.updateCandlesticks = functions.https.onCall(async (data, context) => {
    console.log('updateCandlesticks chamado com:', {
        data: data.data || data,
        auth: context.auth?.uid,
        app: context.app?.appId
    });

    console.log('Variáveis de ambiente:', {
        BRAPI_TOKEN: process.env.BRAPI_TOKEN ? 'Configurado' : 'Não configurado',
        FIREBASE_CONFIG: process.env.FIREBASE_CONFIG
    });

    const params = data.data || data;
    const { ticker, timeframe, brapiToken } = params;

    console.log('Parâmetros extraídos:', { ticker, timeframe, brapiToken: brapiToken ? 'Fornecido' : 'Não fornecido' });
    if (!ticker || !timeframe) {
        console.error('Parâmetros faltantes:', { ticker, timeframe });
        throw new functions.https.HttpsError('invalid-argument', 'Missing required parameters');
    }

    try {
        console.log(`Consultando Brapi API para ticker=${ticker}, interval=${timeframe}, range=3mo`);
        const token = brapiToken || process.env.BRAPI_TOKEN;
        if (!token) {
            console.error('Token da Brapi não configurado');
            throw new functions.https.HttpsError('internal', 'Brapi token not configured');
        }
        console.log(`Usando token Brapi: ${token.substring(0, 5)}...${token.substring(token.length - 5)}`);

        const response = await axios.get(`https://brapi.dev/api/quote/${ticker}`, {
            params: {
                range: '3mo',
                interval: timeframe,
                token: token
            }
        });

        const candlesticks = response.data.results[0].historicalDataPrice.map(candle => ({
            time: new Date(candle.date).getTime(),
            open: candle.open,
            high: candle.high,
            low: candle.low,
            close: candle.close,
            volume: candle.volume
        }));

        console.log(`Dados recebidos da Brapi: ${candlesticks.length} candlesticks`);
        const batch = admin.firestore().batch();
        candlesticks.forEach(candle => {
            const docRef = admin.firestore()
                .collection('candlesticks')
                .doc(ticker)
                .collection(timeframe)
                .doc(candle.time.toString());
            batch.set(docRef, candle);
        });
        await batch.commit();

        console.log(`Dados salvos no Firestore para ${ticker}-${timeframe}`);
        return candlesticks;
    } catch (error) {
        console.error('Erro ao atualizar candlesticks:', error.message, error.response?.data || error);
        throw new functions.https.HttpsError('internal', `Failed to update candlesticks: ${error.message}`);
    }
});