// BackEnd/Auth/config/database.js
const { Sequelize } = require('sequelize');
require('dotenv').config();

console.log(process.env.DATABASE_URL); // Log the URL

const sequelize = new Sequelize(process.env.DATABASE_URL, {
  dialect: 'postgres',
  protocol: 'postgres',
  logging: false, // Disable logging; default: console.log
});

module.exports = sequelize;