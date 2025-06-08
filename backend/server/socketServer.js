const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const mongoose = require('mongoose');
const PaymentModel = require('../models/paymentModel.js'); // tvoj model

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  cors: { origin: "*" },  // prilagodi za produkciju
});

// Middleware za autentifikaciju tokena preko socket.io (primer)
io.use((socket, next) => {
  const token = socket.handshake.auth.token;
  if (!token) {
    return next(new Error("Unauthorized"));
  }
  // Ovde dekodiraj i validiraj token (npr jwt.verify)
  // Ako validno, postavi korisnika u socket
  socket.user = { userId: "pretpostavimo" }; 
  next();
});

io.on('connection', (socket) => {
  console.log('Novi klijent povezan:', socket.id, 'User:', socket.user.userId);

  // Kada se klijent poveže, pošalji mu aktivna plaćanja
  async function sendActivePayments() {
    try {
      const payments = await PaymentModel.find({
        user: socket.user.userId,
        payment_status: 'completed',
        hidden: false,
        // ovde možeš filter za isteka
      }).lean();

      // Dodaj expirations (pretpostavimo da je trajanje u satima i postoji polje date)
      const now = Date.now();
      const paymentsWithExpires = payments.map(p => {
        const expiresAt = new Date(p.date).getTime() + p.duration * 60 * 60 * 1000;
        return {
          vehiclePlate: p.vehicle_plate,
          expiresAt,
          amount: p.amount,
          date: p.date,
        };
      }).filter(p => p.expiresAt > now); // samo aktivna

      socket.emit('activePayments', paymentsWithExpires);
    } catch (err) {
      console.error('Greška pri dohvatu aktivnih plaćanja:', err);
    }
  }

  sendActivePayments();

  // Primer emitovanja da se desilo novo placanje (moguce integrisati sa event emitterom)
  socket.on('paymentMade', async (paymentData) => {
    // Obavesti samo tog korisnika (ili emituj globalno)
    io.to(socket.id).emit('newPayment', paymentData);
  });

  socket.on('disconnect', () => {
    console.log('Klijent se odjavio:', socket.id);
  });
});

const PORT = process.env.PORT || 3002;
server.listen(PORT, () => {
  console.log(`Server radi na portu ${PORT}`);
});
