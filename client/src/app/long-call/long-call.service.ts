import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';

import { Observable } from 'rxjs/Observable';

import { TimeoutParameter } from './TimeoutParameter';

@Injectable()
export class LongCallService {
  private submitURL = 'long-call/submit';
  private pollURL = 'long-call/poll';

  constructor(private http: HttpClient) {}

  submit(body: TimeoutParameter): Observable<HttpResponse<any>> {
    return this.http.post<HttpResponse<any>>(this.submitURL, body, {observe: 'response'});
  }

  poll(taskId: string): Observable<HttpResponse<string[]>> {
    return this.http.get<string[]>(`${this.pollURL}/${taskId}`, {observe: 'response'});
  }
}
