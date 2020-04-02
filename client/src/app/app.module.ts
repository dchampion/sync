import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
// import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpClientModule } from '@angular/common/http';

// used to create fake backend
// import { fakeBackendProvider } from './helpers';

import { appRoutingModule } from './app.routing';
// import { JwtInterceptor, ErrorInterceptor } from './helpers';
import { AppComponent } from './app.component';
import { LongCallComponent } from './long-call/long-call.component';
import { AuthenticatedComponent } from './authenticated';
import { LoginComponent } from './login';
import { RegisterComponent } from './register';
import { AlertComponent } from './components';
import { HomeComponent } from './home';

@NgModule({
    imports: [
        BrowserModule,
        ReactiveFormsModule,
        FormsModule,
        HttpClientModule,
        appRoutingModule
    ],
    declarations: [
        AppComponent,
        HomeComponent,
        AuthenticatedComponent,
        LoginComponent,
        RegisterComponent,
        AlertComponent,
        LongCallComponent
    ],
    providers: [
        /*{ provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
        { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },

        // provider used to create fake backend
        fakeBackendProvider*/
    ],
    bootstrap: [AppComponent]
})
export class AppModule {}
