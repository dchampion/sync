import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { User } from '../models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private registerUrl = 'users/register';

  constructor(private http: HttpClient) {}

  getAll() {
    return this.http.get<User[]>(`${config.apiUrl}/users`);
  }

  register(user: User) {
    return this.http.post(this.registerUrl, user, {responseType: 'text'});
  }

  delete(id: number) {
    return this.http.delete(`${config.apiUrl}/users/${id}`);
  }
}
