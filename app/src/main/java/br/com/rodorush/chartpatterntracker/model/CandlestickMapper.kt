package br.com.rodorush.chartpatterntracker.model

fun Candlestick.toEntity(ticker: String, timeframe: String, lastUpdated: Long = System.currentTimeMillis()): CandlestickEntity {
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

fun List<Candlestick>.toEntities(ticker: String, timeframe: String, lastUpdated: Long = System.currentTimeMillis()): List<CandlestickEntity> {
    return map { it.toEntity(ticker, timeframe, lastUpdated) }
}

fun List<CandlestickEntity>.toCandlesticks(): List<Candlestick> {
    return map { it.toCandlestick() }
}