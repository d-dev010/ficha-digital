import { Directive, ElementRef, HostListener, Input, OnInit } from '@angular/core';
import { NgControl } from '@angular/forms';

/**
 * Diretiva de máscara de input reutilizável.
 * Suporta dois tipos via [mask] input:
 *   - 'cpf'      → 000.000.000-00
 *   - 'telefone' → (00) 00000-0000 ou (00) 0000-0000
 *
 * Uso:
 *   <input matInput formControlName="cpf" mask="cpf">
 *   <input matInput formControlName="telefone" mask="telefone">
 *
 * O valor gravado no FormControl é a string JÁ formatada (ex: "119 9999-9999").
 * O backend deve sanitizar (remover não-dígitos) antes de persistir.
 */
@Directive({
  selector: '[mask]',
  standalone: true,
})
export class InputMaskDirective implements OnInit {
  @Input('mask') maskType: 'cpf' | 'telefone' = 'telefone';

  constructor(private el: ElementRef<HTMLInputElement>, private control: NgControl) {}

  ngOnInit() {
    // aplica a máscara ao valor inicial (se já houver um valor no controle)
    const initialValue = this.control?.value;
    if (initialValue) {
      this.applyMask(initialValue);
    }
  }

  @HostListener('input', ['$event'])
  onInput(event: InputEvent) {
    const raw = this.el.nativeElement.value;
    this.applyMask(raw);
  }

  @HostListener('blur')
  onBlur() {
    // força atualização do control ao sair do campo
    const raw = this.el.nativeElement.value;
    this.applyMask(raw);
  }

  private applyMask(value: string) {
    const digits = value.replace(/\D/g, '');
    let masked = '';

    if (this.maskType === 'cpf') {
      masked = this.maskCpf(digits);
    } else {
      masked = this.maskTelefone(digits);
    }

    this.el.nativeElement.value = masked;
    // atualiza o FormControl com o valor mascarado
    this.control?.control?.setValue(masked, { emitEvent: false });
  }

  private maskCpf(digits: string): string {
    // formato: 000.000.000-00 (máx 11 dígitos)
    const d = digits.slice(0, 11);
    if (d.length <= 3) return d;
    if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`;
    if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`;
    return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
  }

  private maskTelefone(digits: string): string {
    // suporta celular (11 dígitos) e fixo (10 dígitos)
    const d = digits.slice(0, 11);
    if (d.length <= 2) return d.length === 0 ? '' : `(${d}`;
    if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
    if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
    // celular com 9º dígito
    return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
  }
}
