import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AdminService, CriarFarmaciaAdminRequest, CriarUsuarioAdminRequest } from '../../core/services/admin.service';
import { AuthService } from '../../core/auth/auth.service';
import { FarmaciaAdmin, UsuarioAdmin } from '../../core/models/usuario.model';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatTabsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatTableModule, MatChipsModule,
    MatSnackBarModule, MatProgressSpinnerModule, MatTooltipModule
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent implements OnInit {

  private adminService = inject(AdminService);
  public authService   = inject(AuthService);
  private fb           = inject(FormBuilder);
  private snack        = inject(MatSnackBar);

  // ─── Estado ───────────────────────────────────────────────────
  farmacias  = signal<FarmaciaAdmin[]>([]);
  usuarios   = signal<UsuarioAdmin[]>([]);
  carregando = signal(false);

  totalFarmacias = computed(() => this.farmacias().length);
  totalUsuarios  = computed(() => this.usuarios().length);
  totalAtivos    = computed(() => this.usuarios().filter((u: UsuarioAdmin) => u.ativo).length);

  // ─── UI state ─────────────────────────────────────────────────
  mostrarFormFarmacia = signal(false);
  mostrarFormUsuario  = signal(false);
  farmaciaFiltro      = signal<string | null>(null);
  alterandoSenhaId    = signal<string | null>(null);

  colunasFarmacias = ['nome', 'cnpj', 'totalUsuarios', 'acoes'];
  colunasUsuarios  = ['nome', 'email', 'perfil', 'farmacia', 'status', 'acoes'];

  // ─── Forms ────────────────────────────────────────────────────
  formFarmacia: FormGroup;
  formUsuario: FormGroup;
  formSenha: FormGroup;

  constructor() {
    // Aquecimento: acorda a API do Render (plano gratuito hiberna após ~15 min).
    // Disparado ao carregar o painel admin; erro silencioso para não afetar a UX.
    fetch('/health').catch(() => {});

    this.formFarmacia = this.fb.group({
      nomeFarmacia: ['', Validators.required],
      cnpj: [''],
      nomeDono: ['', Validators.required],
      emailDono: ['', [Validators.required, Validators.email]],
      senhaDono: ['', [Validators.required, Validators.minLength(8)]]
    });

    this.formUsuario = this.fb.group({
      nome: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      senha: ['', [Validators.required, Validators.minLength(8)]],
      perfil: ['ATENDENTE', Validators.required],
      farmaciaId: ['', Validators.required]
    });

    this.formSenha = this.fb.group({
      novaSenha: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  ngOnInit(): void {
    this.carregarTudo();
  }

  carregarTudo(): void {
    this.carregando.set(true);
    this.adminService.listarFarmacias().subscribe({
      next: (f: FarmaciaAdmin[]) => {
        this.farmacias.set(f);
        this.carregarUsuarios();
      },
      error: () => {
        this.snack.open('Erro ao carregar farmácias.', 'Fechar', { duration: 4000 });
        this.carregando.set(false);
      }
    });
  }

  carregarUsuarios(): void {
    const filtro = this.farmaciaFiltro();
    this.adminService.listarUsuarios(filtro ?? undefined).subscribe({
      next: (u: UsuarioAdmin[]) => {
        this.usuarios.set(u);
        this.carregando.set(false);
      },
      error: () => {
        this.snack.open('Erro ao carregar usuários.', 'Fechar', { duration: 4000 });
        this.carregando.set(false);
      }
    });
  }

  // ─── Farmácias ────────────────────────────────────────────────

  salvarFarmacia(): void {
    if (this.formFarmacia.invalid) return;
    const req = this.formFarmacia.value as CriarFarmaciaAdminRequest;
    this.adminService.criarFarmacia(req).subscribe({
      next: (f: FarmaciaAdmin) => {
        this.farmacias.update(list => [f, ...list]);
        this.formFarmacia.reset();
        this.mostrarFormFarmacia.set(false);
        this.snack.open(`Farmácia "${f.nome}" criada com sucesso!`, 'OK', { duration: 4000 });
      },
      error: (err: { error?: { detail?: string } }) => {
        const msg = err?.error?.detail ?? 'Erro ao criar farmácia.';
        this.snack.open(msg, 'Fechar', { duration: 5000 });
      }
    });
  }

  excluirFarmacia(farmacia: FarmaciaAdmin): void {
    if (!confirm(`Excluir "${farmacia.nome}" e TODOS os seus dados? Esta ação é irreversível.`)) return;
    this.adminService.excluirFarmacia(farmacia.id).subscribe({
      next: () => {
        this.farmacias.update(list => list.filter((f: FarmaciaAdmin) => f.id !== farmacia.id));
        this.usuarios.update(list => list.filter((u: UsuarioAdmin) => u.farmaciaId !== farmacia.id));
        this.snack.open('Farmácia excluída.', 'OK', { duration: 3000 });
      },
      error: () => this.snack.open('Erro ao excluir farmácia.', 'Fechar', { duration: 4000 })
    });
  }

  filtrarPorFarmacia(id: string | null): void {
    this.farmaciaFiltro.set(id);
    this.carregarUsuarios();
  }

  // ─── Usuários ─────────────────────────────────────────────────

  salvarUsuario(): void {
    if (this.formUsuario.invalid) return;
    const req = this.formUsuario.value as CriarUsuarioAdminRequest;
    this.adminService.criarUsuario(req).subscribe({
      next: (u: UsuarioAdmin) => {
        this.usuarios.update(list => [u, ...list]);
        this.formUsuario.reset({ perfil: 'ATENDENTE' });
        this.mostrarFormUsuario.set(false);
        this.snack.open(`Usuário "${u.nome}" criado!`, 'OK', { duration: 4000 });
      },
      error: (err: { error?: { detail?: string } }) => {
        const msg = err?.error?.detail ?? 'Erro ao criar usuário.';
        this.snack.open(msg, 'Fechar', { duration: 5000 });
      }
    });
  }

  toggleStatus(usuario: UsuarioAdmin): void {
    this.adminService.alterarStatus(usuario.id, !usuario.ativo).subscribe({
      next: (u: UsuarioAdmin) => {
        this.usuarios.update(list => list.map((x: UsuarioAdmin) => x.id === u.id ? u : x));
        this.snack.open(`Usuário ${u.ativo ? 'ativado' : 'desativado'}.`, 'OK', { duration: 3000 });
      },
      error: () => this.snack.open('Erro ao alterar status.', 'Fechar', { duration: 4000 })
    });
  }

  salvarSenha(usuarioId: string): void {
    if (this.formSenha.invalid) return;
    const novaSenha = this.formSenha.value.novaSenha as string;
    this.adminService.alterarSenha(usuarioId, novaSenha).subscribe({
      next: () => {
        this.alterandoSenhaId.set(null);
        this.formSenha.reset();
        this.snack.open('Senha alterada com sucesso!', 'OK', { duration: 3000 });
      },
      error: () => this.snack.open('Erro ao alterar senha.', 'Fechar', { duration: 4000 })
    });
  }

  excluirUsuario(usuario: UsuarioAdmin): void {
    if (!confirm(`Excluir o usuário "${usuario.nome}"?`)) return;
    this.adminService.excluirUsuario(usuario.id).subscribe({
      next: () => {
        this.usuarios.update(list => list.filter((u: UsuarioAdmin) => u.id !== usuario.id));
        this.snack.open('Usuário excluído.', 'OK', { duration: 3000 });
      },
      error: () => this.snack.open('Erro ao excluir usuário.', 'Fechar', { duration: 4000 })
    });
  }

  logout(): void {
    this.authService.logout();
  }

  getNomeFarmacia(id: string | undefined): string {
    if (!id) return '—';
    return this.farmacias().find((f: FarmaciaAdmin) => f.id === id)?.nome ?? id;
  }

  perfilLabel(p: string): string {
    const map: Record<string, string> = { DONO: 'Dono', ATENDENTE: 'Atendente', SUPER_ADMIN: 'Super Admin' };
    return map[p] ?? p;
  }
}
