package br.com.rodorush.chartpatterntracker.model

/** @param lastUpdated instante da leitura na Brapi, como em [CandlestickEntity.lastUpdated]. */
fun Candlestick.toEntity(ticker: String, timeframe: String, lastUpdated: Long): CandlestickEntity {
    return CandlestickEntity(
        ticker = ticker,
        timeframe = timeframe,
        time = time,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = volume,
        lastUpdated = lastUpdated
    )
}

fun CandlestickEntity.toCandlestick(): Candlestick {
    return Candlestick(
        time = time,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = volume
    )
}

/** @param lastUpdated instante da leitura na Brapi, como em [CandlestickEntity.lastUpdated]. */
fun List<Candlestick>.toEntities(ticker: String, timeframe: String, lastUpdated: Long): List<CandlestickEntity> {
    return map { it.toEntity(ticker, timeframe, lastUpdated) }
}

fun List<CandlestickEntity>.toCandlesticks(): List<Candlestick> {
    return map { it.toCandlestick() }
}