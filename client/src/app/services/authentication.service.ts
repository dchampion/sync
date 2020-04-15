import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { User } from '../models';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {
  public currentUser: Observable<User>;
  private currentUserSubject: BehaviorSubject<User>;
  private user: User;

  constructor(private http: HttpClient) {
    this.currentUserSubject = new BehaviorSubject<User>(
      this.user
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): User {
    return this.currentUserSubject.value;
  }

  login(username: string, password: string) {
    return this.http
      .post<any>('/users/authenticate', { username, password })
      .pipe(
        map(user => {
          this.user = user;
          this.currentUserSubject.next(user);
          return user;
        })
      );
  }

  logout() {
    this.currentUserSubject.next(null);
  }
}
