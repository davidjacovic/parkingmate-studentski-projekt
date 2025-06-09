import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import AddCommentForm from './AddCommentForm';

function LocationItem({ loc, user }) {
  const [commentsVisible, setCommentsVisible] = useState(false);
  const [comments, setComments] = useState([]);
  const [loadingComments, setLoadingComments] = useState(false);
  const [error, setError] = useState('');

  const fetchComments = () => {
    setLoadingComments(true);
    setError('');
    fetch(`http://localhost:3002/reviews/parking/${loc._id}`)
      .then(res => res.json())
      .then(data => {
        setComments(data);
        setLoadingComments(false);
      })
      .catch(() => {
        setError('Greška pri učitavanju komentara.');
        setLoadingComments(false);
      });
  };

  const toggleComments = () => {
    if (!commentsVisible && comments.length === 0) {
      fetchComments();
    }
    setCommentsVisible(!commentsVisible);
  };

  const reloadComments = () => {
    setLoadingComments(true);
    fetch(`http://localhost:3002/reviews/parking/${loc._id}`)
      .then(res => res.json())
      .then(data => {
        setComments(data);
        setLoadingComments(false);
        setError('');
      })
      .catch(() => {
        setError('Greška pri učitavanju komentara.');
        setLoadingComments(false);
      });
  };

  const handleDeleteComment = (commentId) => {
    if (!window.confirm('Da li ste sigurni da želite da obrišete komentar?')) return;

    const token = localStorage.getItem('token');
    fetch(`http://localhost:3002/reviews/${commentId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => {
        if (!res.ok) throw new Error('Brisanje neuspešno');
        reloadComments();
      })
      .catch(() => {
        alert('Greška pri brisanju komentara.');
      });
  };
function renderStars(rating) {
  const starsCount = Math.round(rating); // Zaokruži ocenu ako nije ceo broj
  const stars = [];
  for (let i = 0; i < starsCount; i++) {
    stars.push(<span key={i} className="star">⭐</span>);
  }
  return stars;
}

  const handleDeleteParking = () => {
    if (!window.confirm('Da li ste sigurni da želite da obrišete ovu parking lokaciju?')) return;

    const token = localStorage.getItem('token');
    fetch(`http://localhost:3002/parkingLocations/${loc._id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
      .then(res => {
        if (!res.ok) throw new Error('Greška pri brisanju lokacije.');
        alert('Parking lokacija uspešno obrisana.');
        window.location.reload();
      })
      .catch(err => {
        console.error(err);
        alert('Greška pri brisanju lokacije.');
      });
  };

  return (
    <div className="location-card">
      <div className="card-header">
        <h4>
          <Link to={`/location/${loc._id}`} className="location-link">
            {loc.name}
          </Link>
          <button
            type="button"
            className="comment-toggle-btn"
            onClick={toggleComments}
            title={commentsVisible ? "Sakrij komentare" : "Prikaži komentare"}
          >
            💬
          </button>
          {user?.user_type === 'admin' && (
            <button
              type="button"
              className="delete-parking-btn"
              onClick={handleDeleteParking}
              title="Obriši lokaciju"
            >
              🗑
            </button>
          )}
        </h4>
      </div>

      {commentsVisible && (
        <div className="comments-section">
          {loadingComments && <p className="loading-text">Učitavanje komentara...</p>}
          {error && <p className="error-text">{error}</p>}

          {!loadingComments && comments.length === 0 && (
            <p className="no-comments-text">Nema komentara za ovo parking mesto.</p>
          )}

          {!loadingComments && comments.length > 0 && comments.map(c => (
            <div key={c._id} className="comment-item">
              <div className="comment-header">
                <b>{c.user?.username || 'Anonimni korisnik'}</b>
                <span className="comment-rating">{renderStars(c.rating)}</span>
                {user?.user_type === 'admin' && (
                  <button
                    className="delete-comment-btn"
                    onClick={() => handleDeleteComment(c._id)}
                    title="Obriši komentar"
                  >
                    🗑
                  </button>
                )}
              </div>
              <p className="comment-text">{c.review_text}</p>
              <small className="comment-date">{new Date(c.review_date).toLocaleDateString()}</small>
            </div>
          ))}

          {user?.user_type === 'user' ? (
            <AddCommentForm parkingId={loc._id} onNewComment={reloadComments} />
          ) : user?.user_type === 'admin' ? null : (
            <p className="please-login-text">Prijavite se da biste dodali komentar.</p>
          )}
        </div>
      )}
    </div>
  );
}

export default LocationItem;
