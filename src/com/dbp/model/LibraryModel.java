package com.dbp.model;

/*
 * LibraryModel.java
 * Author: Kuang
 */

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.*;

public class LibraryModel {

	// For use in creating dialogs and making them modal
	private JFrame dialogParent;
	private static Connection conn;

	public LibraryModel(JFrame parent, String userid, String password) {
		dialogParent = parent;
		try {
			try {
				Class.forName("org.postgresql.Driver");
			} catch (ClassNotFoundException e) {
				System.out.println("Drive load failure");
			}
			conn = DriverManager.getConnection(
					"jdbc:postgresql://db.ecs.vuw.ac.nz/kuangwt1988_jdbc",userid,password);

			System.out.println("Database connected!");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
			showMessageDialog(dialogParent, "Database connection failure,please check you username and password",
					"Error", ERROR_MESSAGE);
		}
	}

	public void closeDBConnection() {
		try {
			if (!(conn == null)) {
				conn.close();
				System.out.println("Database closed!");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}
	}

	public String bookLookup(int isbn) {
		PreparedStatement ps;
		ResultSet rs;
		String resultStr = "Book Lookup:\n";
		try {
			// get the book information based on isbn
			ps = conn.prepareStatement("select * from book where isbn=?");
			ps.setInt(1, isbn);
			rs = ps.executeQuery();
			while (rs.next()) {
				resultStr = resultStr + isbn + ": " + rs.getString("title")
						+ "\n    Edition: " + rs.getString("edition_no")
						+ " -Number of copies: " + rs.getString("numofcop")
						+ " -Copy left: " + rs.getString("numleft") + "\n";
			}
			rs.close();
			ps.close();
			// get the author names based on isbn
			ps = conn
					.prepareStatement("select * from author where authorid in (select authorid from Book_Author where ISBN=?) order by authorid");
			ps.setInt(1, isbn);
			rs = ps.executeQuery();
			if (resultStr == "Book Lookup:\n") {
				resultStr = "No such ISBN: " + isbn;// define the return if book
													// doesn't exist.
			} else {
				resultStr = resultStr + "Author: ";
				while (rs.next()) {
					resultStr = resultStr + rs.getString("surname").trim();
					if (!(rs.isLast())) {
						resultStr = resultStr + ", ";
					}
				}
				ps.close();
				rs.close();
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}
		return resultStr;
	}

	public String showCatalogue() {
		PreparedStatement ps;
		ResultSet rsBook;
		ResultSet rsAuthor;
		String resultStr = "Show Catalogue:\n";
		try {
			// get all the book information
			ps = conn.prepareStatement("select * from book");
			rsBook = ps.executeQuery();
			while (rsBook.next()) {
				resultStr = resultStr + rsBook.getInt("isbn") + ": "
						+ rsBook.getString("title") + "\n    Edition: "
						+ rsBook.getString("edition_no")
						+ " -Number of copies: " + rsBook.getString("numofcop")
						+ " -Copy left: " + rsBook.getString("numleft") + "\n";
				// get corresponding authors
				ps = conn
						.prepareStatement("select * from author where authorid in (select authorid from Book_Author where ISBN=?) order by authorid");
				ps.setInt(1, rsBook.getInt("isbn"));
				rsAuthor = ps.executeQuery();
				// check the existence of author
				if (rsAuthor.isBeforeFirst()) {
					resultStr = resultStr + "    Author: ";
				} else {
					resultStr += "    No Author Stored \n";
				}
				while (rsAuthor.next()) {
					resultStr = resultStr
							+ rsAuthor.getString("surname").trim();
					if (!(rsAuthor.isLast())) {
						resultStr = resultStr + ", ";
					} else {
						resultStr = resultStr + "\n";
					}
				}
				rsAuthor.close();
			}
			rsBook.close();
			ps.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}
		return resultStr;
	}

	public String showLoanedBooks() {
		PreparedStatement ps;
		ResultSet rsCustBook;
		Set<Integer> isbn = new HashSet<Integer>();
		ResultSet rsBook;
		ResultSet rsCust;
		ResultSet rsAuthor;
		String resultStr = "Show Loaned Books:\n";
		// find the the loaned books
		try {
			ps = conn
					.prepareStatement("select * from book natural join cust_book");
			rsCustBook = ps.executeQuery();
			if (rsCustBook.isBeforeFirst()) {
				while (rsCustBook.next()) {
					isbn.add(rsCustBook.getInt("isbn"));
				}
				rsCustBook.close();
				Iterator<Integer> it = isbn.iterator();
				while (it.hasNext()) {
					// find the books information
					int bookisbn = it.next();
					ps = conn
							.prepareStatement("select * from book where isbn=?");
					ps.setInt(1, bookisbn);
					rsBook = ps.executeQuery();
					while (rsBook.next()) {
						resultStr  += rsBook.getInt("isbn") + ": "
								+ rsBook.getString("title") + "\n"
								+ "    Edition:" + rsBook.getInt("edition_no")
								+ " - Number of copies: "
								+ rsBook.getInt("numofcop")
								+ " - Copies left: " + rsBook.getInt("numleft")
								+ "\n";
					}
					rsBook.close();
					// get the book's authors
					ps = conn
							.prepareStatement("select * from author natural join book_author where isbn=?");
					ps.setInt(1, bookisbn);
					rsAuthor = ps.executeQuery();
					if (rsAuthor.isBeforeFirst()) {
						resultStr += "    Authors: ";
						while (rsAuthor.next()) {
							resultStr += rsAuthor.getString("surname");
							if (!rsAuthor.isLast()) {
								resultStr += ", ";
							} else {
								resultStr += "\n";
							}
						}
					} else {
						resultStr += "    Authors: (No author exist)";
					}
					rsAuthor.close();
					// get the book's borrowers
					ps = conn
							.prepareStatement("select * from customer natural join cust_book where isbn=?");
					ps.setInt(1, bookisbn);
					rsCust = ps.executeQuery();
					if (rsCust.isBeforeFirst()) {
						resultStr += "Borrowers:\n";
						while (rsCust.next()) {
							resultStr += "        "
									+ rsCust.getInt("customerid") + ": "
									+ rsCust.getString("l_name").trim() + ", "
									+ rsCust.getString("f_name").trim() + " - "
									+ rsCust.getString("city") + "\n";
						}
						rsCust.close();
					} else {
						resultStr += "Borrowers: (No borrowers)";
					}
				}
			} else {
				resultStr += "(No Loaned Books)";
			}
			ps.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}

		return resultStr;
	}

	public String showAuthor(int authorID) {
		PreparedStatement ps;
		ResultSet rsBook;
		ResultSet rsAuthor;
		String resultStr = "Show Author:\n";
		try {
			// get the author based on authorID
			ps = conn
					.prepareStatement("select * from author where authorid = ?");
			ps.setInt(1, authorID);
			rsAuthor = ps.executeQuery();
			// define the return if the author doesn't exist
			if (!rsAuthor.isBeforeFirst()) {
				resultStr += "AuthorID: " + authorID + "doesn't exist";
			} else {
				while (rsAuthor.next()) {
					resultStr += "    " + authorID + " - "
							+ rsAuthor.getString("name").trim() + " "
							+ rsAuthor.getString("surname").trim() + "\n";
				}
				rsAuthor.close();
				// get the book written by the author based on authorID
				ps = conn
						.prepareStatement("select * from book natural join book_author where authorid=?");
				ps.setInt(1, authorID);
				rsBook = ps.executeQuery();
				// define the return if the author doesn't exist
				if (rsBook.isBeforeFirst()) {
					resultStr += "    Book written:\n";
				} else {
					resultStr += "No book written";
				}
				while (rsBook.next()) {
					resultStr += "        " + rsBook.getString("isbn") + " - "
							+ rsBook.getString("title") + "\n";
				}
				rsBook.close();
			}

			ps.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}
		return resultStr;
	}

	public String showAllAuthors() {
		PreparedStatement ps;
		ResultSet rsAuthor;
		String resultStr = "Show All Auhotrs:\n";
		try {
			ps = conn.prepareStatement("select * from author");
			rsAuthor = ps.executeQuery();
			if (!rsAuthor.isBeforeFirst()) {
				resultStr += "No Author exist\n";
			}
			while (rsAuthor.next()) {
				resultStr += "    " + rsAuthor.getInt(1) + ": "
						+ rsAuthor.getString(3).trim() + " "
						+ rsAuthor.getString(2).trim() + "\n";
			}
			rsAuthor.close();
			ps.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}
		return resultStr;
	}

	public String showCustomer(int customerID) {
		PreparedStatement ps;
		ResultSet rsBook;
		ResultSet rsCust;
		String resultStr = "Show Customer:\n";
		try {
			ps = conn
					.prepareStatement("select * from customer where customerid=?");
			ps.setInt(1, customerID);
			rsCust = ps.executeQuery();
			if (!rsCust.isBeforeFirst()) {
				resultStr += "    No such customer ID: " + customerID;
			} else {
				while (rsCust.next()) {
					resultStr += "    " + customerID + ": "
							+ rsCust.getString("l_name").trim() + ", "
							+ rsCust.getString("f_name").trim() + " - "
							+ rsCust.getString("city") + "\n";
					ps = conn
							.prepareStatement("select * from cust_book natural join book where customerid=?");
					ps.setInt(1, customerID);
					rsBook = ps.executeQuery();
					if (!rsBook.isBeforeFirst()) {
						resultStr += "    (No book borrowed)";
					} else {
						resultStr += "    Book Borrowed:\n";
						while (rsBook.next()) {
							resultStr += "    " + rsBook.getInt("isbn") + " - "
									+ rsBook.getString("title").trim() + "\n";
						}
					}
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}

		return resultStr;
	}

	public String showAllCustomers() {
		PreparedStatement ps;
		ResultSet rsCust;
		String resultStr = "Show All Customer:\n";
		try {
			ps = conn.prepareStatement("select * from customer");
			rsCust = ps.executeQuery();
			if (!rsCust.isBeforeFirst()) {
				resultStr += "    No customer exists";
			} else {
				while (rsCust.next()) {
					resultStr += "    " + rsCust.getInt("customerid") + ": "
							+ rsCust.getString("l_name").trim() + ", "
							+ rsCust.getString("f_name") + " - "
							+ rsCust.getString("city") + "\n";
				}
			}
			rsCust.close();
			ps.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}
		return resultStr;
	}

	public String borrowBook(int isbn, int customerID, int day, int month,
			int year) {
		Date date = new Date(year, month, day);
		PreparedStatement ps;
		ResultSet rsCust;
		ResultSet rsBook;
		ResultSet rsCustBook;
		String resultStr = "";
		Boolean error = true;
		String title = "";
		String name = "";
		String surname = "";
		try {
			conn.setAutoCommit(false);
			// check the existence of customer and lock it

			ps = conn
					.prepareStatement("select * from customer where customerid =? for update");
			ps.setInt(1, customerID);
			rsCust = ps.executeQuery();
			if (!rsCust.isBeforeFirst()) {
				resultStr = "Borrow Book:\nCustomerid: " + customerID
						+ "doesn't exist\n";
				error = false;
			} else {
				while (rsCust.next()) {
					name = rsCust.getString("l_name").trim();
					surname = rsCust.getString("f_name").trim();
				}
			}
			rsCust.close();
			// check the existence of book and lock it
			ps = conn
					.prepareStatement("select * from book where isbn=? for update");
			ps.setInt(1, isbn);
			rsBook = ps.executeQuery();
			if (!rsBook.isBeforeFirst()) {
				resultStr = "Borrow Book:\n Book: " + isbn + "doesn't exist\n ";
				error = false;
			} else {
				while (rsBook.next()) {
					if (rsBook.getInt("numofcop") <= 0) {
						resultStr = "Borrow Book:\n Not enough copies of book "
								+ isbn + "left\n";
						error = false;
					} else {
						title = rsBook.getString("title").trim();
					}
				}
			}
			rsBook.close();
			// check whether the customer loaned this book.
			ps = conn
					.prepareStatement("select * from cust_book where isbn=? and customerid=?");
			ps.setInt(1, isbn);
			ps.setInt(2, customerID);
			rsCustBook = ps.executeQuery();
			if (rsCustBook.isBeforeFirst()) {
				resultStr = "Borrow Book:\n customer " + customerID
						+ " already has book " + isbn + " on loan";
				error = false;
			}
			rsCustBook.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
			error = false;
		}
		if (error) {
			showMessageDialog(dialogParent,
					"Locked tuples ready to update. Click OK to continue",
					"Pausing", PLAIN_MESSAGE);
			try {
				// Insert the cust_book table
				ps = conn
						.prepareStatement("insert into cust_book values(?,?,?)");
				ps.setInt(1, isbn);
				ps.setDate(2, date);
				ps.setInt(3, customerID);
				ps.executeUpdate();
				// Update the book table
				ps = conn
						.prepareStatement("update book set numofcop = numofcop - 1 where isbn=?");
				ps.setInt(1, isbn);
				ps.executeUpdate();
				conn.commit();
				conn.setAutoCommit(true);
				ps.close();
				resultStr = "Borrow Book:\n    Book: " + isbn + " (" + title
						+ ")\n" + "    Loaned to: " + customerID + " ("
						+ surname + " " + name + ")\n    Due Date:" + date;
			} catch (SQLException e) {
				try {
					conn.rollback();
					showMessageDialog(dialogParent,
							"Update Error, the datebase rolled back",
							"Update Error", ERROR_MESSAGE);
					System.out.println(e.getMessage());
					System.out.println(e.getErrorCode());
				} catch (SQLException e1) {
					System.out.println(e.getMessage());
					System.out.println(e.getErrorCode());
				}
			}
		}
		return resultStr;
	}

	public String returnBook(int isbn, int customerid) {
		PreparedStatement ps;
		ResultSet rsCust;
		ResultSet rsBook;
		ResultSet rsCustBook;
		String resultStr = "";
		Boolean error = true;
		try {
			conn.setAutoCommit(false);
			// check the existence of customer
			ps = conn
					.prepareStatement("select * from customer where customerid=? for update");
			ps.setInt(1, customerid);
			rsCust = ps.executeQuery();
			if (!rsCust.isBeforeFirst()) {
				resultStr = "Return Book:\n    Customer ID: " + customerid
						+ " doesn't exist";
				error = false;
			}
			rsCust.close();
			// check the existence of book
			if (error) {
				ps = conn
						.prepareStatement("select * from book where isbn=? for update");
				ps.setInt(1, isbn);
				rsBook = ps.executeQuery();
				if (!rsBook.isBeforeFirst()) {
					resultStr = "Return Book:\n    No such ISBN: " + isbn;
					error = false;
				}
				rsBook.close();
			}
			// check whether this customer borrowed the book
			if (error) {
				ps = conn
						.prepareStatement("select * from cust_book where customerid=? and isbn=? for update");
				ps.setInt(1, customerid);
				ps.setInt(2, isbn);
				rsCustBook = ps.executeQuery();
				if (!rsCustBook.isBeforeFirst()) {
					resultStr = "Return Book:\n    Book " + isbn
							+ " is not loaned to customer " + customerid;
					error = false;
				}
				rsCustBook.close();
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}
		if (error) {
			showMessageDialog(dialogParent,
					"Locked tuples ready to update. Click OK to continue",
					"Pausing", PLAIN_MESSAGE);
			try {
				// update the book table
				ps = conn
						.prepareStatement("update book set numofcop = numofcop + 1 where isbn=?");
				ps.setInt(1, isbn);
				ps.executeUpdate();
				// delete the cust_book table
				ps = conn
						.prepareStatement("delete from cust_book where customerid=? and isbn=?");
				ps.setInt(1, customerid);
				ps.setInt(2, isbn);
				int rows = ps.executeUpdate();
				conn.commit();
				conn.setAutoCommit(true);
				ps.close();
				resultStr += "Book " + isbn + " returned for customer "
						+ customerid;
				if (rows != 1) {
					conn.rollback();
					showMessageDialog(dialogParent,
							"Update Error, the datebase rolled back",
							"Update Error", ERROR_MESSAGE);
				}
			} catch (SQLException e) {
				try {
					conn.rollback();
					showMessageDialog(dialogParent,
							"Update Error, the datebase rolled back",
							"Update Error", ERROR_MESSAGE);
				} catch (SQLException e1) {
					System.out.println(e.getMessage());
					System.out.println(e.getErrorCode());
				}
				System.out.println(e.getMessage());
				System.out.println(e.getErrorCode());
			}
		}

		return resultStr;
	}

	public String deleteCus(int customerID) {
		PreparedStatement ps;
		ResultSet rsCust;
		ResultSet rsCustBook;
		String resultStr = "Customer " + customerID + " didn't delete";
		try {
			conn.setAutoCommit(false);
			// check the existence of customer
			ps = conn
					.prepareStatement("select * from customer where customerid=? for update");
			ps.setInt(1, customerID);
			rsCust = ps.executeQuery();
			if (rsCust.isBeforeFirst()) {
				// if customer exist check whether the customer borrowed book.
				ps = conn
						.prepareStatement("select * from cust_book where customerid=? for update");
				ps.setInt(1, customerID);
				rsCustBook = ps.executeQuery();
				if (rsCustBook.isBeforeFirst()) {
					resultStr = "Customer " + customerID
							+ "have books on loan, can not be deleted";
				} else {
					// if customer didn't borrow book perform delete
					ps = conn
							.prepareStatement("delete from customer where customerid=?");
					ps.setInt(1, customerID);
					int rows = ps.executeUpdate();
					conn.commit();
					if (rows != 1) {
						conn.rollback();
						showMessageDialog(dialogParent,
								"Update Error, the datebase rolled back",
								"Update Error", ERROR_MESSAGE);
					} else {
						resultStr = "Customer " + customerID
								+ " has been deleted";
					}
				}
				rsCustBook.close();
			} else {
				resultStr = "Customer " + customerID + " doesn't exist";
			}
			rsCust.close();
			ps.close();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			try {
				conn.rollback();
				showMessageDialog(dialogParent,
						"Update Error, the datebase rolled back",
						"Update Error", ERROR_MESSAGE);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}
		return resultStr;
	}

	public String deleteAuthor(int authorID) {
		PreparedStatement ps;
		ResultSet rsAuthor;
		ResultSet rsBook;
		String resultStr = "";
		try {
			conn.setAutoCommit(false);
			// check the existence of customer
			ps = conn.prepareStatement("select * from author where authorid=?");
			ps.setInt(1, authorID);
			rsAuthor = ps.executeQuery();
			ps = conn
					.prepareStatement("select * from book natural join book_author where authorid=?");
			ps.setInt(1, authorID);
			rsBook = ps.executeQuery();
			if (rsAuthor.isBeforeFirst()) {
				// if author exist, delete it.
				ps = conn
						.prepareStatement("delete from author where authorid=?");
				ps.setInt(1, authorID);
				int rows = ps.executeUpdate();
				conn.commit();
				conn.setAutoCommit(true);
				if (rows != 1) {
					conn.rollback();
					showMessageDialog(dialogParent,
							"Delete Error, the dat1ebase rolled back",
							"Delete Error", ERROR_MESSAGE);
				} else {
					resultStr = "Author " + authorID + " has been deleted\n";
					// check the author whether written books
					while (rsBook.next()) {
						resultStr += "Book " + rsBook.getInt("isbn") + " - "
								+ rsBook.getString("title").trim()
								+ " has been set a default author\n";
					}
				}
			} else {
				resultStr = "Authorid " + authorID + " doesn't exist";
			}
			rsAuthor.close();

		} catch (SQLException e) {
			try {
				conn.rollback();
				showMessageDialog(
						dialogParent,
						"Delete Error more than one author been set to default, the datebase rolled back",
						"Delete Error", ERROR_MESSAGE);
			} catch (SQLException e1) {
				System.out.println(e.getMessage());
				System.out.println(e.getErrorCode());
			}
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}
		return resultStr;
	}

	public String deleteBook(int isbn) {
		PreparedStatement ps;
		ResultSet rsAuthor;
		ResultSet rsBook;
		ResultSet rsBook_Author;
		String resultStr = "";
		try {
			conn.setAutoCommit(false);
			// find the author of the book
			ps = conn
					.prepareStatement("select * from book natural join book_author where isbn=?");
			ps.setInt(1, isbn);
			rsBook_Author = ps.executeQuery();
			// check the existence of book
			ps = conn.prepareStatement("select * from book where isbn=?");
			ps.setInt(1, isbn);
			rsBook = ps.executeQuery();
			if (rsBook.isBeforeFirst()) {
				// if the book exist, then check the book's author and the
				// existence of default author.
				ps = conn
						.prepareStatement("select * from book natural join book_author where isbn=? and authorid=0");
				ps.setInt(1, isbn);
				rsAuthor = ps.executeQuery();
				if (rsAuthor.isBeforeFirst()) {
					// if default author exist, this book cannot be delete
					resultStr = "Book " + isbn
							+ " has a default author, cannot be delete";
				} else {
					// delete the book
					ps = conn.prepareStatement("delete from book where isbn=?");
					ps.setInt(1, isbn);
					int rows = ps.executeUpdate();
					conn.commit();
					conn.setAutoCommit(true);
					if (rows != 1) {
						conn.rollback();
						showMessageDialog(dialogParent,
								"Delete Error, the datebase rolled back",
								"Delete Error", ERROR_MESSAGE);
					}
					resultStr = "Book " + isbn + " has been deleted\n";
					// show the author that set to default book
					while (rsBook_Author.next()) {
						resultStr += "    Author "
								+ rsBook_Author.getString("name").trim()
								+ " has been set to default book \n";
					}
				}
			} else {
				resultStr = "Book " + isbn + " doesn't exist";
			}
			rsBook.close();
		} catch (SQLException e) {
			try {
				conn.rollback();
				showMessageDialog(
						dialogParent,
						"Delete Error more than one author been set to default, the datebase rolled back",
						"Delete Error", ERROR_MESSAGE);
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println(e.getMessage());
			System.out.println(e.getErrorCode());
		}

		return resultStr;
	}

}