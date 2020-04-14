import { Component, OnInit } from '@angular/core';
import { first } from 'rxjs/operators';
import { Router } from '@angular/router';

import { User } from '../models';
import { AlertService, UserService, AuthenticationService } from '../services';

@Component({ templateUrl: 'authenticated.component.html' })
export class AuthenticatedComponent implements OnInit {
  currentUser: User;
  users = [];

  constructor(
    private authenticationService: AuthenticationService,
    private userService: UserService,
    private router: Router,
    private alertService: AlertService) {

    this.currentUser = this.authenticationService.currentUserValue;
  }

  ngOnInit() {
    this.loadAllUsers();
  }

  delete(user) {
    this.userService
      .delete(user)
      .pipe(first())
      .subscribe(() => this.loadAllUsers());
  }

  private loadAllUsers() {
    this.userService
      .getAll()
      .pipe(first())
      .subscribe(users => (this.users = users));
  }

  logout() {
    this.authenticationService.logout();
    this.alertService.success('You have logged out', true);
    this.router.navigate(['/login']);
  }
}
