import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { catchError, of } from 'rxjs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ClientesService } from '../clientes.service';
import { ClienteDetalhe } from '../../../core/models/cliente.model';
import { ExtratoItem } from '../../../core/models/pagamento.model';
import { CurrencyBrPipe } from '../../../shared/pipes/currency-br.pipe';
import { LancarFiadoDialogComponent } from './lancar-fiado-dialog.component';
import { RegistrarPagamentoDialogComponent } from './registrar-pagamento-dialog.component';
import { EditarTelefoneDialogComponent } from './editar-telefone-dialog.component';

@Component({
  selector: 'app-cliente-extrato',
  standalone: true,
  imports: [
    CommonModule, MatToolbarModule, MatButtonModule, MatIconModule,
    MatCardModule, MatListModule, MatDividerModule, MatProgressSpinnerModule,
    MatDialogModule, MatChipsModule, MatTooltipModule, CurrencyBrPipe,
  ],
  templateUrl: './cliente-extrato.component.html',
  styleUrl: './cliente-extrato.component.scss',
})
export class ClienteExtratoComponent implements OnInit {
  clienteId = '';
  cliente = signal<ClienteDetalhe | null>(null);
  extrato = signal<ExtratoItem[]>([]);
  carregando = signal(true);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private clientesService: ClientesService,
    private dialog: MatDialog,
  ) {}

  ngOnInit() {
    this.clienteId = this.route.snapshot.paramMap.get('id')!;
    this.carregar();
  }

  carregarErro = signal<string | null>(null);

  carregar() {
    this.carregando.set(true);
    this.carregarErro.set(null);
    forkJoin({
      cliente: this.clientesService.detalhe(this.clienteId).pipe(catchError(() => of(null))),
      extrato: this.clientesService.extrato(this.clienteId).pipe(catchError(() => of(null))),
    }).subscribe(({ cliente, extrato }) => {
      if (!cliente) {
        this.carregarErro.set('Não foi possível carregar os dados do cliente.');
      } else {
        this.cliente.set(cliente);
        this.extrato.set(extrato?.content ?? []);
      }
      this.carregando.set(false);
    });
  }

  abrirLancarFiado() {
    const ref = this.dialog.open(LancarFiadoDialogComponent, {
      width: '440px',
      data: { clienteId: this.clienteId, nomeCliente: this.cliente()?.nome },
    });
    ref.afterClosed().subscribe(lancou => { if (lancou) this.carregar(); });
  }

  abrirRegistrarPagamento() {
    const ref = this.dialog.open(RegistrarPagamentoDialogComponent, {
      width: '440px',
      data: { clienteId: this.clienteId, nomeCliente: this.cliente()?.nome, saldoAtual: this.cliente()?.saldoDevedor },
    });
    ref.afterClosed().subscribe(registrou => { if (registrou) this.carregar(); });
  }

  abrirEditarTelefone() {
    const ref = this.dialog.open(EditarTelefoneDialogComponent, {
      width: '400px',
      data: {
        clienteId: this.clienteId,
        nomeCliente: this.cliente()?.nome,
        telefoneAtual: this.cliente()?.telefone,
      },
    });
    ref.afterClosed().subscribe(editou => {
      if (editou) {
        // Update the client state with the new details returned from the backend
        this.cliente.set(editou);
      }
    });
  }

  voltar() {
    this.router.navigate(['/clientes']);
  }
}
