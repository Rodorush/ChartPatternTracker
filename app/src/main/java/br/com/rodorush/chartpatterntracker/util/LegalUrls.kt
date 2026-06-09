package br.com.rodorush.chartpatterntracker.util

/**
 * Fonte única das URLs dos documentos legais (Política de Privacidade e Termos de Uso),
 * hospedados no Firebase Hosting do projeto `chartpatterntracker`.
 *
 * As páginas vivem em `hosting/` na raiz do repositório; publicar com
 * `firebase deploy --only hosting`. Se o domínio mudar, basta trocar [BASE_URL] aqui.
 *
 * [DATA_DELETION] aponta para a seção de exclusão de dados da Política e é a URL
 * declarada na Play Console (seção *Data deletion*), casando com o fluxo de exclusão
 * de conta in-app (NF-03e).
 */
object LegalUrls {
    private const val BASE_URL = "https://chartpatterntracker.web.app"

    const val PRIVACY = "$BASE_URL/privacy.html"
    const val TERMS = "$BASE_URL/terms.html"
    const val DATA_DELETION = "$PRIVACY#data-deletion"
}
