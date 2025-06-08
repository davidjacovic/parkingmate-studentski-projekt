var createError = require('http-errors');
var express = require('express');
var path = require('path');
var cookieParser = require('cookie-parser');
var logger = require('morgan');
require('dotenv').config();
//console.log('Mongo URL:', process.env.MONGO_URL);
const mongoose = require('mongoose');
const session = require('express-session');
const MongoStore = require('connect-mongo');
const cors = require('cors')
const jwt = require('jsonwebtoken');
const SECRET = process.env.JWT_SECRET;
const cron = require('node-cron');
const { exec } = require('child_process');


const app = express();

//CORS
const allowedOrigins = ['http://localhost:3000', 'http://localhost:3002'];
app.use(cors({
  credentials: true,
  origin(origin, callback) {
    if (!origin) return callback(null, true); // allow REST clients like Postman with no origin
    if (!allowedOrigins.includes(origin)) {
      return callback(new Error('The CORS policy does not allow access from the specified Origin.'), false);
    }
    return callback(null, true);
  }
}));

// Database connection
mongoose.set('strictQuery', false);
const mongoDB = process.env.MONGO_URL;
mongoose.connect(mongoDB, {
  useNewUrlParser: true,
  useUnifiedTopology: true,
});
mongoose.Promise = global.Promise;
const db = mongoose.connection;
db.on('error', console.error.bind(console, 'MongoDB connection error:'));
db.once('open', () => {
  console.log('MongoDB connected');
});

// Session configuration
app.use(session({
  secret: 'work hard', // Consider using process.env.SESSION_SECRET in production
  secret: 'work hard',
  resave: true,
  saveUninitialized: false,
  store: MongoStore.create({
    mongoUrl: mongoDB,
    collectionName: 'sessions'
  }),
  cookie: {
    maxAge: 1000 * 60 * 60 * 24 // 1 day
  }
}));

// Make session accessible in views
app.use((req, res, next) => {
  res.locals.session = req.session;
  next();
});
//Pokretanje skripte u bin
cron.schedule('*/5 * * * *', () => {
  exec('node ./bin/parkingLogScript.js', (err, stdout, stderr) => {
    if (err) {
      console.error(`Cron error: ${err.message}`);
    }
    if (stderr) {
      console.error(`Cron stderr: ${stderr}`);
    }
    console.log(`Cron stdout: ${stdout}`);
  });
});
// Routers
const indexRouter = require('./routes/index');
const vehicleRouter = require('./routes/vehicleRoutes');
const usersRouter = require('./routes/userRoutes');
const tariffRouter = require('./routes/tariffRoutes');
const subscribersRouter = require('./routes/subscribersRoutes');
const reviewsRouter = require('./routes/reviewsRoutes');
const paymentRouter = require('./routes/paymentRoutes');
const parkingLocationRouter = require('./routes/parkingLocationRoutes');

// View engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'hbs');

app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

const { authenticateToken } = require('./middlewares/auth');


// Mounting routers
app.use('/', indexRouter);
app.use('/vehicles', authenticateToken, vehicleRouter);
app.use('/users', usersRouter);
app.use('/tariffs', tariffRouter);
app.use('/subscribers',authenticateToken,  subscribersRouter);
app.use('/reviews', reviewsRouter);
app.use('/payments', authenticateToken, paymentRouter);
app.use('/parkingLocations', parkingLocationRouter);

// Catch 404 and forward to error handler
app.use(function(req, res, next) {
  next(createError(404));
});

// Error handler
app.use(function(err, req, res, next) {
  res.locals.message = err.message;
  res.locals.error = req.app.get('env') === 'development' ? err : {};

  res.status(err.status || 500);
  res.render('error');
});

module.exports = app;


