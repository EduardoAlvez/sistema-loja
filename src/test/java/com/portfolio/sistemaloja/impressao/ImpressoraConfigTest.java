package com.portfolio.sistemaloja.impressao;

import com.portfolio.sistemaloja.service.ValidacaoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ImpressoraConfigTest {

    @AfterEach
    void limparPropriedades() {
        System.clearProperty("impressora.nome");
        System.clearProperty("impressora.codepage");
        System.clearProperty("impressora.qr.modulo");
    }

    @Test
    void normalizaONomeEModulo() {
        assertEquals("", new ImpressoraConfig("   ", 2, 4).nome());
        assertEquals("POS-80", new ImpressoraConfig("  POS-80  ", 16, 6).nome());
    }

    @Test
    void recusaCodepageEModuloInvalidos() {
        assertThrows(ValidacaoException.class, () -> new ImpressoraConfig("POS", 3, 4));
        assertThrows(ValidacaoException.class, () -> new ImpressoraConfig("POS", 2, 0));
        assertThrows(ValidacaoException.class, () -> new ImpressoraConfig("POS", 2, 17));
    }

    @Test
    void leConfiguracaoDasPropriedades() {
        System.setProperty("impressora.nome", " POS-80 ");
        System.setProperty("impressora.codepage", "16");
        System.setProperty("impressora.qr.modulo", "6");

        ImpressoraConfig config = ImpressoraConfig.padrao();

        assertEquals("POS-80", config.nome());
        assertEquals(16, config.codepage());
        assertEquals(6, config.moduloQr());
    }

    @Test
    void usaOsPadroesQuandoNaoHaConfiguracao() {
        assumeTrue(System.getenv("IMPRESSORA_CODEPAGE") == null, "ambiente define a codepage");
        assumeTrue(System.getenv("IMPRESSORA_QR_MODULO") == null, "ambiente define o modulo");

        ImpressoraConfig config = ImpressoraConfig.padrao();

        assertEquals(2, config.codepage());
        assertEquals(4, config.moduloQr());
    }

    @Test
    void falhaComNumeroInvalido() {
        System.setProperty("impressora.codepage", "abc");

        assertThrows(ValidacaoException.class, ImpressoraConfig::padrao);
    }
}
