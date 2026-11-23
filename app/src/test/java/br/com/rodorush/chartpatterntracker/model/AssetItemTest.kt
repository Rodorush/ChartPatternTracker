package br.com.rodorush.chartpatterntracker.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetItemTest {

    /** Valores do `/quote/list` e do `/quote/WEGE3` da Brapi, lidos em 22/11/2026. */
    @Test
    fun shortNameIgualAoTickerCaiNoNomeDaLista() {
        val asset = AssetItem(ticker = "WEGE3", shortName = "WEGE3", longName = "WEG SA", name = "WEG S.A.")
        assertEquals("WEG S.A.", asset.displayName)
    }

    /** A maioria dos BDRs vem do `/quote/list` com o ticker no `name`, como o AAPL34. */
    @Test
    fun bdrSemNomeNaListaFicaVazio() {
        val asset = AssetItem(ticker = "AAPL34", shortName = "AAPL34", longName = "AAPL34", name = "AAPL34")
        assertEquals("", asset.displayName)
    }

    @Test
    fun shortNamePreenchidoVemPrimeiro() {
        val asset = AssetItem(ticker = "PETR4", shortName = "Petrobras PN", longName = "Petróleo Brasileiro S.A.", name = "Petrobras")
        assertEquals("Petrobras PN", asset.displayName)
    }

    @Test
    fun semShortNameValeONameESemOsDoisOLongName() {
        assertEquals("Vale", AssetItem(ticker = "VALE3", longName = "Vale S.A.", name = "Vale").displayName)
        assertEquals("Vale S.A.", AssetItem(ticker = "VALE3", longName = "Vale S.A.").displayName)
    }

    @Test
    fun comparacaoComOTickerIgnoraCaixaEEspacosNasPontas() {
        val asset = AssetItem(ticker = "WEGE3", shortName = " wege3 ", name = "WEG S.A.")
        assertEquals("WEG S.A.", asset.displayName)
    }

    @Test
    fun semNenhumNomeFicaVazio() {
        assertEquals("", AssetItem(ticker = "WEGE3").displayName)
        assertEquals("", AssetItem(ticker = "WEGE3", shortName = "  ", name = "\t").displayName)
    }

    @Test
    fun espacosRepetidosViramUmSo() {
        val asset = AssetItem(ticker = "B3SA3", name = "  B3   S.A. -\tBRASIL  ")
        assertEquals("B3 S.A. - BRASIL", asset.displayName)
    }
}
