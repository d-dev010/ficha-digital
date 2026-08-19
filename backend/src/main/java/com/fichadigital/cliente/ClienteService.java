package com.fichadigital.cliente;

import com.fichadigital.farmacia.Farmacia;
import com.fichadigital.farmacia.FarmaciaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
     * Busca clientes por nome (parcial), telefone ou CPF — US04.
     * CPF mascarado no resultado (RNF04).
     *
     * @param farmaciaId UUID da farmácia — extraído do JWT (RNF03)
     * @param termo      Termo de busca
     * @return Lista de ClienteResumo com CPF mascarado
     */
    @Transactional(readOnly = true)
    public List<ClienteResumo> buscar(UUID farmaciaId, String termo) {
        return clienteRepository.buscar(farmaciaId, termo)
                .stream()
                .map(c -> ClienteResumo.from(c, mascarar(c.getCpf())))
                .toList();
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
     * Mascara o CPF para listagens: "123.456.789-00" → "123.***.**\*-00" (RNF04).
     * Aceita CPF com ou sem formatação.
     */
    static String mascarar(String cpf) {
        if (cpf == null || cpf.isBlank()) return null;
        // Remove formatação para normalizar
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) return "***.***.***-**";
        return digits.substring(0, 3) + ".***.***-" + digits.substring(9);
    }
}
