import { Pipe, PipeTransform } from '@angular/core';

/**
 * Pipe de formatação de moeda BRL.
 * Centralizado para evitar duplicação nas telas 2, 3 e 4 (conforme instrução 3 do doc frontend).
 * Uso: {{ cliente.saldoDevedor | currencyBr }}
 */
@Pipe({ name: 'currencyBr', standalone: true })
export class CurrencyBrPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value == null) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  }
}
