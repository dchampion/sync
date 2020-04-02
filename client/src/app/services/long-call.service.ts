import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';

import { Observable } from 'rxjs/Observable';

import { TimeoutParameter } from '../long-call/TimeoutParameter';

@Injectable()
export class LongCallService {
  private submitURL = 'long-call/submit';
  private pollURL = 'long-call/poll';

  constructor(private http: HttpClient) {}

  getSubmitPath(): string {
    return this.submitURL;
  }

  getPollPath(taskId: string): string {
    return `${this.pollURL}/${taskId}`;
  }

  submit(body: TimeoutParameter): Observable<HttpResponse<any>> {
    return this.http.post<HttpResponse<any>>(this.getSubmitPath(), body, {observe: 'response'});
  }

  poll(taskId: string): Observable<HttpResponse<string[]>> {
    return this.http.get<string[]>(this.getPollPath(taskId), {observe: 'response'});
  }
}
