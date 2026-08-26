package com.fichadigital.cliente;

/**
 * Request para atualização do telefone do cliente.
 * Permite valor nulo para remover o telefone.
 */
public record AtualizarTelefoneRequest(String telefone) {}
