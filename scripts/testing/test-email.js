const nodemailer = require('nodemailer');

// Configuración del transportador de Gmail
const transporter = nodemailer.createTransport({
  service: 'gmail',
  host: 'smtp.gmail.com',
  port: 587,
  secure: false,
  auth: {
    user: 'jorge.gangale@mtn.cl',
    pass: 'sltwfedqspqmgkis' // App password sin espacios
  }
});

// Generar código de verificación
const verificationCode = Math.floor(100000 + Math.random() * 900000);

// Configuración del correo
const mailOptions = {
  from: '"Sistema de Admisión MTN" <jorge.gangale@mtn.cl>',
  to: 'jorge.gangale@gmail.com', // Tu correo personal para probar
  subject: 'Código de Verificación - Sistema de Admisión MTN',
  html: `
    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
      <div style="background-color: #1e40af; color: white; padding: 20px; text-align: center;">
        <h1>Sistema de Admisión MTN</h1>
      </div>
      <div style="padding: 30px; background-color: #f9f9f9;">
        <h2>Código de Verificación</h2>
        <p>Hola,</p>
        <p>Tu código de verificación es:</p>
        <div style="background-color: #e5e5e5; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 5px; margin: 20px 0;">
          ${verificationCode}
        </div>
        <p>Este código expira en 10 minutos.</p>
        <p>Si no solicitaste este código, puedes ignorar este correo.</p>
        <hr style="border: none; border-top: 1px solid #ddd; margin: 30px 0;">
        <p style="color: #666; font-size: 12px;">
          Este es un correo automático, por favor no responder.
        </p>
      </div>
      <div style="background-color: #1e40af; color: white; padding: 10px; text-align: center; font-size: 12px;">
        © 2024 Colegio Monte Tabor y Nazaret
      </div>
    </div>
  `
};

// Enviar el correo
console.log('📧 Enviando correo de verificación...');
console.log('🔑 Código de verificación:', verificationCode);

transporter.sendMail(mailOptions, (error, info) => {
  if (error) {
    console.error('❌ Error enviando correo:', error);
  } else {
    console.log('✅ Correo enviado exitosamente!');
    console.log('📬 ID del mensaje:', info.messageId);
    console.log('📧 Respuesta del servidor:', info.response);
  }
});