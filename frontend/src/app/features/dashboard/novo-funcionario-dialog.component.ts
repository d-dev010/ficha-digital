import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FuncionariosService } from './funcionarios.service';

@Component({
  selector: 'app-novo-funcionario-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Novo Funcionário</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-grid">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Nome *</mat-label>
          <input matInput formControlName="nome" id="novo-func-nome" placeholder="Nome do funcionário">
          @if (form.controls.nome.hasError('required')) {
            <mat-error>Nome é obrigatório</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>E-mail *</mat-label>
          <input matInput formControlName="email" id="novo-func-email" placeholder="email@exemplo.com" type="email">
          @if (form.controls.email.hasError('required')) {
            <mat-error>E-mail é obrigatório</mat-error>
          } @else if (form.controls.email.hasError('email')) {
            <mat-error>E-mail inválido</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Senha Temporária *</mat-label>
          <input matInput formControlName="senhaTemporaria" id="novo-func-senha" placeholder="Mínimo 8 caracteres" type="password">
          @if (form.controls.senhaTemporaria.hasError('required')) {
            <mat-error>Senha é obrigatória</mat-error>
          } @else if (form.controls.senhaTemporaria.hasError('minlength')) {
            <mat-error>Mínimo de 8 caracteres</mat-error>
          }
        </mat-form-field>

        @if (erro()) {
          <div class="erro">{{ erro() }}</div>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close id="btn-cancelar-novo-func">Cancelar</button>
      <button mat-flat-button color="primary" id="btn-salvar-novo-func"
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
export class NovoFuncionarioDialogComponent {
  private fb = inject(FormBuilder);
  form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    senhaTemporaria: ['', [Validators.required, Validators.minLength(8)]],
  });

  salvando = signal(false);
  erro = signal<string | null>(null);

  constructor(
    private funcionariosService: FuncionariosService,
    private dialogRef: MatDialogRef<NovoFuncionarioDialogComponent>,
  ) {}

  salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    const dados = this.form.getRawValue();
    this.funcionariosService.cadastrar(dados).subscribe({
      next: funcionario => this.dialogRef.close(funcionario),
      error: () => {
        this.salvando.set(false);
        this.erro.set('Erro ao cadastrar funcionário. Verifique os dados.');
      },
    });
  }
}
