package com.fichadigital.cliente;

import com.fichadigital.farmacia.Farmacia;
import com.fichadigital.farmacia.FarmaciaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service de clientes.
 *
 * Regras implementadas:
 *  - RNF03: farmaciaId sempre vem do JWT/SecurityContext — nunca do body
 *  - RNF04: CPF mascarado nas listagens ("123.***.**#-00"), completo apenas no detalhe
 *  - US04: busca por nome (parcial), telefone ou CPF
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    /** Regex pré-compilada para remover caracteres não-numéricos do CPF (Problema 4 — Performance). */
    private static final Pattern NON_DIGIT = Pattern.compile("\\D");

    private final ClienteRepository clienteRepository;
    private final FarmaciaRepository farmaciaRepository;

    /**
     * Cadastra um novo cliente na farmácia do usuário autenticado (US03).
     *
     * @param farmaciaId UUID da farmácia — extraído do JWT (RNF03)
     * @param request    Dados do cliente
     */
    @Transactional
    public Cliente cadastrar(UUID farmaciaId, CadastrarClienteRequest request) {
        Farmacia farmacia = farmaciaRepository.findById(farmaciaId)
                .orElseThrow(() -> new EntityNotFoundException("Farmácia não encontrada"));

        Cliente cliente = Cliente.builder()
                .farmacia(farmacia)
                .nome(request.nome())
                .telefone(request.telefone())
                .cpf(request.cpf())
                .build();

        return clienteRepository.save(cliente);
    }

    /**
     * Busca paginada de clientes por nome (parcial), telefone ou CPF — US04 (Problema 2 — Performance).
     * CPF mascarado no resultado (RNF04). Ordenação definida pelo Pageable (ex: nome ASC).
     *
     * @param farmaciaId UUID da farmácia — extraído do JWT (RNF03)
     * @param termo      Termo de busca
     * @param pageable   Configuração de página e ordenação
     * @return Página de ClienteResumo com CPF mascarado
     */
    @Transactional(readOnly = true)
    public Page<ClienteResumo> buscar(UUID farmaciaId, String termo, Pageable pageable) {
        return clienteRepository.buscar(farmaciaId, termo, pageable)
                .map(c -> ClienteResumo.from(c, mascarar(c.getCpf())));
    }

    /**
     * Detalhe do cliente (CPF completo — apenas no detalhe, conforme RNF04).
     *
     * @param farmaciaId UUID da farmácia — extraído do JWT (RNF03)
     * @param clienteId  UUID do cliente
     * @return ClienteDetalhe com CPF completo
     */
    @Transactional(readOnly = true)
    public ClienteDetalhe detalhe(UUID farmaciaId, UUID clienteId) {
        Cliente cliente = clienteRepository.findByIdAndFarmaciaId(clienteId, farmaciaId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));
        return ClienteDetalhe.from(cliente);
    }

    /**
     * Atualiza o telefone de um cliente (US — editar telefone).
     * Permite valor nulo para remover o telefone.
     *
     * @param farmaciaId UUID da farmácia — extraído do JWT (RNF03)
     * @param clienteId  UUID do cliente
     * @param request    Novo telefone (pode ser nulo)
     * @return ClienteDetalhe atualizado
     */
    @Transactional
    public ClienteDetalhe atualizarTelefone(UUID farmaciaId, UUID clienteId, AtualizarTelefoneRequest request) {
        Cliente cliente = clienteRepository.findByIdAndFarmaciaId(clienteId, farmaciaId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));
        cliente.setTelefone(request.telefone() != null && !request.telefone().isBlank()
                ? request.telefone()
                : null);
        return ClienteDetalhe.from(clienteRepository.save(cliente));
    }


    /**
     * Mascara o CPF para listagens: "123.456.789-00" → "123.***.**\*-00" (RNF04).
     * Aceita CPF com ou sem formatação.
     */
    static String mascarar(String cpf) {
        if (cpf == null || cpf.isBlank()) return null;
        // Remove formatação para normalizar — usa Pattern pré-compilado (evita recompilação por chamada)
        String digits = NON_DIGIT.matcher(cpf).replaceAll("");
        if (digits.length() != 11) return "***.***.***-**";
        return digits.substring(0, 3) + ".***.***-" + digits.substring(9);
    }
}
