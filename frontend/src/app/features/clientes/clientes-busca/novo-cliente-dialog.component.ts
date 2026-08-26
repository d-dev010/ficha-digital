import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClientesService } from '../clientes.service';
import { InputMaskDirective } from '../../../shared/directives/input-mask.directive';

@Component({
  selector: 'app-novo-cliente-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule, InputMaskDirective,
  ],
  template: `
    <h2 mat-dialog-title>Novo Cliente</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-grid">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Nome *</mat-label>
          <input matInput formControlName="nome" id="novo-cliente-nome" placeholder="Nome completo">
          @if (form.controls.nome.hasError('required')) {
            <mat-error>Nome é obrigatório</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Telefone</mat-label>
          <input matInput formControlName="telefone" id="novo-cliente-telefone"
                 mask="telefone" placeholder="(11) 99999-9999" inputmode="tel">
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>CPF (opcional)</mat-label>
          <input matInput formControlName="cpf" id="novo-cliente-cpf"
                 mask="cpf" placeholder="000.000.000-00" inputmode="numeric">
        </mat-form-field>

        @if (erro()) {
          <div class="erro">{{ erro() }}</div>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close id="btn-cancelar-novo-cliente">Cancelar</button>
      <button mat-flat-button color="primary" id="btn-salvar-novo-cliente"
              [disabled]="form.invalid || salvando()" (click)="salvar()">
        @if (salvando()) { <mat-spinner diameter="18"></mat-spinner> }
        Cadastrar
      </button>
    </mat-dialog-actions>
  `,
  styles: [`.form-grid { display: flex; flex-direction: column; gap: 4px; padding-top: 8px; min-width: 360px; }
            .full-width { width: 100%; }
            .erro { color: #c62828; font-size: 13px; padding: 4px 0; }`],
})
export class NovoClienteDialogComponent {
  private fb = inject(FormBuilder);
  form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    telefone: [''],
    cpf: [''],
  });

  salvando = signal(false);
  erro = signal<string | null>(null);

  constructor(
    private clientesService: ClientesService,
    private dialogRef: MatDialogRef<NovoClienteDialogComponent>,
  ) {}

  salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    const { nome, telefone, cpf } = this.form.getRawValue();
    this.clientesService.cadastrar({ nome, telefone: telefone || undefined, cpf: cpf || undefined }).subscribe({
      next: cliente => this.dialogRef.close(cliente),
      error: () => {
        this.salvando.set(false);
        this.erro.set('Erro ao cadastrar cliente. Tente novamente.');
      },
    });
  }
}
