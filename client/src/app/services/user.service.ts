import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { User } from '../models';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UserService {

  constructor(private http: HttpClient) {}

  getAll() {
    return this.http.get<User[]>('/users');
  }

  register(user: User, passwordLeakChecked: boolean) {
    return this.http.post('users/register', user,
      {responseType: 'text', headers: {'Password-Leak-Checked': `${passwordLeakChecked}`}});
  }

  isPasswordLeaked(password: string): Observable<boolean> {
    return this.http.post<boolean>('/users/is-pw-leaked', password);
  }

  delete(user: User) {
    return this.http.delete(`users/${user.username}`);
  }
}
