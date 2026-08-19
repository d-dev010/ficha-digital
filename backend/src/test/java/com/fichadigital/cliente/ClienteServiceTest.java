package com.fichadigital.cliente;

import com.fichadigital.farmacia.Farmacia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para ClienteService.
 * Foco: mascaramento de CPF (RNF04) — testado independentemente da infra.
 */
class ClienteServiceTest {

    @Test
    @DisplayName("Deve mascarar CPF com formatação padrão")
    void deveMascararCpfFormatado() {
        String resultado = ClienteService.mascarar("123.456.789-00");
        assertThat(resultado).isEqualTo("123.***.***-00");
    }

    @Test
    @DisplayName("Deve mascarar CPF sem formatação (só dígitos)")
    void deveMascararCpfSemFormatacao() {
        String resultado = ClienteService.mascarar("12345678900");
        assertThat(resultado).isEqualTo("123.***.***-00");
    }

    @Test
    @DisplayName("Deve retornar null quando CPF é null")
    void deveRetornarNullParaCpfNull() {
        assertThat(ClienteService.mascarar(null)).isNull();
    }

    @Test
    @DisplayName("Deve retornar null quando CPF está em branco")
    void deveRetornarNullParaCpfEmBranco() {
        assertThat(ClienteService.mascarar("   ")).isNull();
    }

    @Test
    @DisplayName("Deve retornar máscara genérica para CPF com número errado de dígitos")
    void deveRetornarMascaraGenericaParaCpfInvalido() {
        assertThat(ClienteService.mascarar("123456")).isEqualTo("***.***.***-**");
    }

    @ParameterizedTest
    @CsvSource({
            "000.000.000-00, 000.***.***-00",
            "999.999.999-99, 999.***.***-99",
            "111.222.333-44, 111.***.***-44"
    })
    @DisplayName("Deve mascarar corretamente diferentes CPFs")
    void deveMascararDiferentesCpfs(String cpfEntrada, String esperado) {
        assertThat(ClienteService.mascarar(cpfEntrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("ClienteResumo.from() deve conter CPF mascarado")
    void clienteResumodeveConterCpfMascarado() {
        Farmacia farmacia = Farmacia.builder().id(UUID.randomUUID()).nome("Farmácia X").build();
        Cliente cliente = Cliente.builder()
                .id(UUID.randomUUID())
                .farmacia(farmacia)
                .nome("Ana Lima")
                .telefone("11999999999")
                .cpf("123.456.789-00")
                .saldoDevedor(new BigDecimal("50.00"))
                .build();

        String cpfMascarado = ClienteService.mascarar(cliente.getCpf());
        ClienteResumo resumo = ClienteResumo.from(cliente, cpfMascarado);

        assertThat(resumo.cpfMascarado()).isEqualTo("123.***.***-00");
        assertThat(resumo.nome()).isEqualTo("Ana Lima");
        assertThat(resumo.saldoDevedor()).isEqualByComparingTo("50.00");
    }
}
