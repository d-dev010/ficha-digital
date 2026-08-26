import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(4)]],
  });

  carregando = signal(false);
  erro = signal<string | null>(null);
  senhaVisivel = signal(false);

  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}

  entrar() {
    if (this.form.invalid) return;
    this.carregando.set(true);
    this.erro.set(null);

    const { email, senha } = this.form.getRawValue();
    this.auth.login(email, senha).subscribe({
      next: () => this.router.navigate(['/clientes']),
      error: (err) => {
        this.carregando.set(false);
        if (err.status === 401) {
          this.erro.set('E-mail ou senha incorretos.');
        } else if (err.status === 429) {
          this.erro.set('Muitas tentativas. Aguarde alguns minutos.');
        } else {
          this.erro.set('Erro ao conectar com o servidor. Tente novamente.');
        }
      },
    });
  }
}
