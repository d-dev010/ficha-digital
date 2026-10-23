import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';

export interface SelecionarMesDialogData {
  /** Título exibido no dialog. Ex: "Exportar CSV do Mês" */
  titulo: string;
}

export interface SelecionarMesDialogResult {
  mes: number;
  ano: number;
}

/**
 * Dialog reutilizável para seleção de mês e ano antes de exportar CSV.
 * Retorna { mes, ano } ou undefined se o usuário cancelar.
 *
 * Uso:
 *   const ref = this.dialog.open(SelecionarMesDialogComponent, {
 *     width: '92vw', maxWidth: '360px',
 *     data: { titulo: 'Exportar CSV do Mês' },
 *   });
 *   ref.afterClosed().subscribe((result: SelecionarMesDialogResult | undefined) => { ... });
 */
@Component({
  selector: 'app-selecionar-mes-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatDialogModule, MatButtonModule,
    MatSelectModule, MatFormFieldModule, MatIconModule,
  ],
  template: `
    <div class="dialog-header">
      <mat-icon class="dialog-icon">calendar_month</mat-icon>
      <h2 mat-dialog-title>{{ data.titulo }}</h2>
    </div>

    <mat-dialog-content>
      <p class="dialog-hint">Selecione o mês e o ano para filtrar o relatório.</p>

      <div class="selects-row">
        <mat-form-field appearance="outline" class="select-mes">
          <mat-label>Mês</mat-label>
          <mat-select [(ngModel)]="mesSelecionado" id="select-mes">
            @for (m of meses; track m.valor) {
              <mat-option [value]="m.valor">{{ m.nome }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="select-ano">
          <mat-label>Ano</mat-label>
          <mat-select [(ngModel)]="anoSelecionado" id="select-ano">
            @for (a of anos; track a) {
              <mat-option [value]="a">{{ a }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close id="btn-cancelar-mes">Cancelar</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="!mesSelecionado || !anoSelecionado"
        (click)="confirmar()"
        id="btn-confirmar-mes">
        <mat-icon>download</mat-icon>
        Exportar
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-header {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 20px 24px 0;
    }
    .dialog-icon {
      color: #1565c0;
      font-size: 26px;
      width: 26px;
      height: 26px;
    }
    [mat-dialog-title] {
      margin: 0;
      font-size: 18px;
      font-weight: 700;
      color: #1a237e;
    }
    .dialog-hint {
      font-size: 13px;
      color: #757575;
      margin: 4px 0 16px;
    }
    .selects-row {
      display: flex;
      gap: 12px;
    }
    .select-mes { flex: 2; }
    .select-ano { flex: 1; }
    mat-dialog-actions {
      padding: 8px 16px 16px;
    }
  `],
})
export class SelecionarMesDialogComponent {
  mesSelecionado: number;
  anoSelecionado: number;

  readonly meses = [
    { valor: 1,  nome: 'Janeiro' },
    { valor: 2,  nome: 'Fevereiro' },
    { valor: 3,  nome: 'Março' },
    { valor: 4,  nome: 'Abril' },
    { valor: 5,  nome: 'Maio' },
    { valor: 6,  nome: 'Junho' },
    { valor: 7,  nome: 'Julho' },
    { valor: 8,  nome: 'Agosto' },
    { valor: 9,  nome: 'Setembro' },
    { valor: 10, nome: 'Outubro' },
    { valor: 11, nome: 'Novembro' },
    { valor: 12, nome: 'Dezembro' },
  ];

  readonly anos: number[];

  constructor(
    public dialogRef: MatDialogRef<SelecionarMesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SelecionarMesDialogData,
  ) {
    const hoje = new Date();
    this.mesSelecionado = hoje.getMonth() + 1; // mês atual como padrão
    this.anoSelecionado = hoje.getFullYear();  // ano atual como padrão

    // Oferece os últimos 3 anos até o atual
    const anoAtual = hoje.getFullYear();
    this.anos = [anoAtual, anoAtual - 1, anoAtual - 2];
  }

  confirmar(): void {
    const result: SelecionarMesDialogResult = {
      mes: this.mesSelecionado,
      ano: this.anoSelecionado,
    };
    this.dialogRef.close(result);
  }
}
