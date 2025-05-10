const functions = require('firebase-functions');
const admin = require('firebase-admin');
const axios = require('axios');

admin.initializeApp();

exports.updateCandlesticks = functions.https.onCall(async (data, context) => {
    const { ticker, timeframe, startDate, endDate } = data;
    if (!ticker || !timeframe || !startDate || !endDate) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing required parameters');
    }

    try {
        const response = await axios.get(`https://brapi.dev/api/quote/${ticker}/history`, {
            params: {
                range: '3mo',
                interval: timeframe,
                start: startDate,
                end: endDate
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

        return candlesticks;
    } catch (error) {
        console.error('Error updating candlesticks:', error);
        throw new functions.https.HttpsError('internal', 'Failed to update candlesticks');
    }
});